package com.etsviewer.core.homework

import com.etsviewer.core.answers.AnswerDocument
import com.etsviewer.core.resources.*
import com.etsviewer.core.feedback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class HomeworkReadPhase { NOT_DOWNLOADED, DOWNLOADING, PROCESSING, READY, FAILED }
data class HomeworkReadState(
    val phase: HomeworkReadPhase = HomeworkReadPhase.NOT_DOWNLOADED,
    val progress: ResourceProgress? = null, val document: AnswerDocument? = null, val failure: AppFailure? = null,
) {
    val isReadable: Boolean get() = document?.isReadable == true
    val isPartial: Boolean get() = isReadable && document?.isComplete == false
}

class HomeworkReadingController(private val scope: CoroutineScope, private val loader: RemoteResourceLoader) {
    private val lock = Any()
    private var generation = 0L
    private val jobs = mutableMapOf<String, Deferred<AnswerDocument>>()
    private val mutable = MutableStateFlow<Map<String, HomeworkReadState>>(emptyMap())
    val states = mutable.asStateFlow()

    fun status(entry: HomeworkEntry, baseUrl: String) = states.value[entry.readingKey(baseUrl)] ?: HomeworkReadState()

    suspend fun read(entry: HomeworkEntry, baseUrl: String, retryFailed: Boolean = false): AnswerDocument {
        val key = entry.readingKey(baseUrl)
        val task = synchronized(lock) {
            val cached = mutable.value[key]
            if (jobs[key] == null && cached?.isReadable == true && (!retryFailed || cached.document?.isComplete == true)) return cached.document!!
            jobs[key] ?: run {
                val version = generation
                lateinit var work: Deferred<AnswerDocument>
                fun publish(record: HomeworkReadState) = synchronized(lock) {
                    if (generation == version && jobs[key] === work) mutable.update { it + (key to record) }
                }
                work = scope.async(start = CoroutineStart.LAZY) {
                    try {
                        publish(HomeworkReadState(HomeworkReadPhase.PROCESSING, ResourceProgress("检查缓存", 0f, HomeworkReadPhase.PROCESSING), cached?.document))
                        val result = loader.load(entry, baseUrl, cached?.document, retryFailed) { progress ->
                            publish(HomeworkReadState(progress.phase, progress, cached?.document))
                        }
                        currentCoroutineContext().ensureActive()
                        val phase = if (result.isReadable) HomeworkReadPhase.READY else HomeworkReadPhase.FAILED
                        publish(HomeworkReadState(phase, ResourceProgress(if (phase == HomeworkReadPhase.READY) "可读取" else "读取失败，可重试", 1f, phase),
                            result, result.resourceFailures.firstOrNull()?.failure))
                        result
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        publish(HomeworkReadState(if (cached?.isReadable == true) HomeworkReadPhase.READY else HomeworkReadPhase.FAILED,
                            document = cached?.document, failure = FailureClassifier.classify(e, FeedbackOperation.DOWNLOAD)))
                        throw e
                    } finally { synchronized(lock) { if (jobs[key] === work) jobs.remove(key) } }
                }
                jobs[key] = work
                work
            }
        }
        task.start()
        return task.await()
    }

    fun cancel(entry: HomeworkEntry, baseUrl: String) {
        val key = entry.readingKey(baseUrl)
        val work = synchronized(lock) {
            val existing = jobs.remove(key)
            if (existing != null) mutable.update { states ->
                val record = states[key]
                if (record?.isReadable == true) states + (key to record.copy(phase = HomeworkReadPhase.READY, progress = null)) else states - key
            }
            existing
        }
        work?.cancel()
    }

    fun reset() {
        val active = synchronized(lock) {
            generation++
            val active = jobs.values.toList()
            jobs.clear(); mutable.value = emptyMap()
            active
        }
        active.forEach { it.cancel() }
    }
}
