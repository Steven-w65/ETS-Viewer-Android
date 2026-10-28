package com.etsviewer.core.resources

import com.etsviewer.core.answers.*
import com.etsviewer.core.homework.*
import com.etsviewer.core.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File

data class ResourceProgress(val message: String, val fraction: Float, val phase: HomeworkReadPhase = HomeworkReadPhase.DOWNLOADING)
fun interface ResourceFetcher {
    suspend fun download(url: String, destination: File, progress: (Long, Long) -> Unit)
}
class HttpResourceFetcher(client: OkHttpClient = standardHttpClient()) : ResourceFetcher {
    private val transport = EtsCdnCompatibilityTransport(client)
    override suspend fun download(url: String, destination: File, progress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        transport.useDownloadResponse(url) { response ->
                if (!response.isSuccessful) throw HttpFailure(response.code)
                val body = response.body ?: throw ProtocolFailure("Empty resource response")
                val length = body.contentLength()
                var count = 0L
                destination.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read); count += read; progress(count, length)
                        }
                    }
                }
                if (length >= 0 && count != length) throw ProtocolFailure("Incomplete resource download")
                if (count == 0L) throw ProtocolFailure("Empty resource download")
        }
    }
}

class RemoteResourceLoader(
    private val cache: CloudResourceCache, private val processor: PackageProcessor = PackageProcessor(),
    private val decoder: AnswerDataDecoder = AnswerDataDecoder(), private val fetcher: ResourceFetcher = HttpResourceFetcher(),
    private val resolveUrl: (String, String) -> String = ::resolve,
) {
    suspend fun load(homework: HomeworkEntry, baseUrl: String, previous: AnswerDocument? = null, retryFailed: Boolean = false,
        progress: (ResourceProgress) -> Unit): AnswerDocument = withContext(Dispatchers.IO) { cache.mutex.withLock {
        val pages = linkedMapOf<String, ResourceReaderPage>()
        val attempted = mutableMapOf<String, Int>()
        val failures = mutableListOf<ResourceReadFailure>()
        val reusable = previous?.takeIf { it.readingId == homework.readingKey(baseUrl) }
        val failedIds = reusable?.resourceFailures?.map { it.resourceId }?.toSet().orEmpty()
        val oldPages = reusable?.resourcePages?.associateBy { it.resourceId }.orEmpty()
        val retryIndices = reusable?.resourceFailures?.map { it.originalGroupIndex }?.toSet().orEmpty()
        val totalWork = (if (reusable == null) homework.resources.size else retryIndices.size).coerceAtLeast(1)
        var completedWork = 0
        homework.resources.forEachIndexed { index, resource ->
            val countsWork = reusable == null || index in retryIndices
            currentCoroutineContext().ensureActive()
            var id = digest("SHA-256", (baseUrl + "\n" + resource.url).toByteArray(Charsets.UTF_8))
            try {
                val url = resolveUrl(baseUrl, resource.url)
                val slot = cache.slot(url); id = slot.name
                val backup = File(cache.root, slot.name + ".backup")
                if (!slot.exists() && backup.isDirectory) check(backup.renameTo(slot)) { "Unable to recover cached resource" }
                val firstIndex = attempted.putIfAbsent(id, index)
                if (firstIndex != null) {
                    if (id in pages) ResourceIntegrity.verify(File(slot, "package.zip"), resource.checksums)
                    failures.firstOrNull { it.resourceId == id && it.originalGroupIndex == firstIndex }?.let { failures += it.copy(groupName = resource.name, originalGroupIndex = index) }
                    return@forEachIndexed
                }
                val old = oldPages[id]
                if (reusable != null && id !in failedIds && old != null) {
                    pages[id] = old; return@forEachIndexed
                }
                progress(ResourceProgress("检查缓存", completedWork.toFloat() / totalWork, HomeworkReadPhase.PROCESSING))
                val cached = cachedBundle(slot, resource)
                val fallback = old ?: cached?.takeIf { it.sections.isNotEmpty() }?.let {
                    ResourceReaderPage(id, resource.name, File(slot, "content").canonicalPath, index, it.sections)
                }
                val force = retryFailed && id in failedIds
                if (!force && cached != null && cached.sections.isNotEmpty()) {
                    pages[id] = fallback!!
                    if (cached.errors.isNotEmpty()) failures += ResourceReadFailure(id, resource.name, index, cached.errors.joinToString("; "))
                } else {
                    val pending = File(cache.root, slot.name + ".pending")
                    pending.deleteRecursively(); check(pending.mkdirs())
                    try {
                        val partial = File(pending, "download.part")
                        progress(ResourceProgress("下载资源", completedWork.toFloat() / totalWork))
                        fetcher.download(url, partial) { downloaded, total ->
                            val fraction = if (total > 0) downloaded.toFloat() / total else 0f
                            progress(ResourceProgress("下载资源", (completedWork + fraction * .8f) / totalWork))
                        }
                        currentCoroutineContext().ensureActive()
                        ResourceIntegrity.verify(partial, resource.checksums)
                        val archive = File(pending, "package.zip")
                        check(partial.renameTo(archive)) { "Unable to finish resource download" }
                        progress(ResourceProgress("解压资源", (completedWork + .8f) / totalWork, HomeworkReadPhase.PROCESSING))
                        val extraction = File(pending, "content")
                        processor.extract(archive, extraction)
                        currentCoroutineContext().ensureActive()
                        progress(ResourceProgress("解析资源", (completedWork + .9f) / totalWork, HomeworkReadPhase.PROCESSING))
                        val parsed = parseFolder(archive, extraction, File(slot, "content"), resource.name)
                        if (parsed.sections.isEmpty()) throw ProtocolFailure(parsed.errors.joinToString("; ").ifBlank { "No readable resource content" })
                        if (fallback != null && parsed.errors.isNotEmpty() &&
                            fallback.sections.any { oldSection -> parsed.sections.none { it.sourceContentPath == oldSection.sourceContentPath } })
                            throw ProtocolFailure("Retry would discard previously readable resource content")
                        currentCoroutineContext().ensureActive()
                        File(pending, "complete").writeText(processor.contentFiles(archive, extraction).first().relativeTo(pending).path)
                        publish(pending, slot)
                        pages[id] = ResourceReaderPage(id, resource.name, File(slot, "content").canonicalPath, index, parsed.sections)
                        if (parsed.errors.isNotEmpty()) failures += ResourceReadFailure(id, resource.name, index, parsed.errors.joinToString("; "))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        if (fallback != null) pages[id] = fallback
                        throw e
                    } finally { pending.deleteRecursively() }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failures += ResourceReadFailure(id, resource.name, index, e.message ?: "Resource processing failed") }
            finally { if (countsWork) completedWork++ }
        }
        val extra = if (homework.resources.isEmpty()) listOf("此作业没有可读取的云端资源") else emptyList()
        val result = AnswerDocument.fromResources(homework.name, pages.values.toList(), failures, homework.resources.size, homework.readingKey(baseUrl), extra)
        progress(ResourceProgress(if (result.isComplete) "完成" else if (result.isReadable) "部分资源不可用" else "资源读取失败，可重试", 1f,
            if (result.isReadable) HomeworkReadPhase.READY else HomeworkReadPhase.FAILED))
        result
    } }

    private data class ParsedFolder(val sections: List<AnswerSection>, val errors: List<String>)
    private suspend fun parseFolder(archive: File, extracted: File, publishedRoot: File, group: String): ParsedFolder {
        val sections = mutableListOf<AnswerSection>(); val errors = mutableListOf<String>()
        for (content in processor.contentFiles(archive, extracted)) {
            currentCoroutineContext().ensureActive()
            try {
                val relativeParent = content.parentFile.relativeTo(extracted.canonicalFile).path
                val decoded = decoder.decode(content.readText(Charsets.UTF_8), group)
                sections += decoded.copy(sectionTitle = decoded.sectionTitle ?: sectionTitleFromDirectory(relativeParent),
                    mediaRoot = File(publishedRoot, relativeParent).canonicalPath, mediaBoundary = publishedRoot.canonicalPath,
                    sourceContentPath = content.relativeTo(extracted.canonicalFile).invariantSeparatorsPath)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { errors += e.message ?: "Content parsing failed" }
        }
        return ParsedFolder(sections, errors)
    }
    private suspend fun cachedBundle(slot: File, resource: ResourceGroup): ParsedFolder? {
        if (!File(slot, "complete").isFile) return null
        return try {
            val archive = File(slot, "package.zip")
            ResourceIntegrity.verify(archive, resource.checksums)
            val root = File(slot, "content").canonicalFile
            val markerTarget = File(slot, File(slot, "complete").readText()).canonicalFile
            require(markerTarget.toPath().startsWith(root.toPath()) && markerTarget.isFile)
            parseFolder(archive, root, root, resource.name).takeIf { it.sections.isNotEmpty() }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    }
    private fun publish(pending: File, slot: File) {
        val backup = File(cache.root, slot.name + ".backup")
        backup.deleteRecursively()
        val hadPrevious = slot.exists()
        if (hadPrevious) check(slot.renameTo(backup)) { "Unable to preserve cached resource" }
        if (!pending.renameTo(slot)) {
            if (hadPrevious) check(backup.renameTo(slot)) { "Unable to restore cached resource" }
            throw ProtocolFailure("Unable to publish resource cache")
        }
        backup.deleteRecursively()
    }
    companion object {
        fun resolve(baseUrl: String, resourceUrl: String): String {
            require(resourceUrl.isNotBlank()) { "Missing cloud resource URL" }
            val url = when {
                resourceUrl.startsWith("https://") || resourceUrl.startsWith("http://") -> resourceUrl
                resourceUrl.contains("://") || resourceUrl.startsWith("//") -> throw ProtocolFailure("Unsupported resource URL")
                else -> baseUrl.trimEnd('/') + "/" + resourceUrl.trimStart('/')
            }
            return url.replaceFirst(Regex("^http://"), "https://").toHttpUrl().also {
                require(it.scheme == "https") { "Cloud resources require HTTPS" }
            }.toString()
        }
    }
}
