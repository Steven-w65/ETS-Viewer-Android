package com.etsviewer.android

import com.etsviewer.android.R
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.etsviewer.core.answers.*
import com.etsviewer.core.homework.*
import com.etsviewer.core.identity.*
import com.etsviewer.android.identity.*
import com.etsviewer.core.resources.*
import com.etsviewer.core.service.*
import com.etsviewer.core.feedback.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class HomeworkUiState(
    val batch: HomeworkBatch = HomeworkBatch(), val period: HomeworkPeriod = HomeworkPeriod.CURRENT,
    val query: String = "", val loading: Boolean = false, val feedback: AppFeedback? = null,
    val progress: ResourceProgress? = null, val reader: AnswerDocument? = null,
    val selectedHomework: HomeworkEntry? = null, val selectedBaseUrl: String = "", val clearingCache: Boolean = false,
    val readerRetrying: Boolean = false,
)

private fun productionApi(): CloudApi =
    ServiceConnector(CloudTransport(EtsPayloadCodec(Md5RequestSigner(BuildConfig.ETS_PID, BuildConfig.ETS_SIGNING_SECRET))), BuildConfig.ETS_SERIAL)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val api: CloudApi = productionApi(),
    private val store: KeystoreSessionStore = KeystoreSessionStore(application),
    private val cache: CloudResourceCache = CloudResourceCache(File(application.cacheDir, "cloud-homework")),
    loader: RemoteResourceLoader = RemoteResourceLoader(cache),
) : AndroidViewModel(application) {
    private val access = PhoneAccessController(api, store)
    val identity = access.state
    private val readings = HomeworkReadingController(viewModelScope, loader)
    val readingStates = readings.states
    private val mutable = MutableStateFlow(HomeworkUiState())
    val homework = mutable.asStateFlow()
    private val mutableTheme = MutableStateFlow(store.theme)
    val theme = mutableTheme.asStateFlow()
    private var work: Job? = null
    private var resourceWork: Job? = null
    private var authWork: Job? = null
    private var cacheWork: Job? = null
    private var identityVersion = 0L
    private var listVersion = 0L
    private var selectionVersion = 0L
    var enteredPhone: String = ""; private set
    var enteredPassword: String = ""; private set
    var exportDocument: AnswerDocument? = null

    init {
        viewModelScope.launch {
            var signedIn = false
            var principal: Triple<String, String, String>? = null
            access.state.collect { state ->
                if (state is AccessUiState.SignedIn) {
                    val account = state.session.account
                    val owner = Triple(state.session.phone, account.id.ifBlank { account.key }, account.parentId)
                    if (principal != owner) { clearIdentityState(); principal = owner }
                    enteredPassword = ""
                    if (!signedIn && !mutable.value.loading) refresh()
                    signedIn = true
                } else {
                    signedIn = false
                    if (state is AccessUiState.SignedOut || state is AccessUiState.Failure) {
                        if (principal != null) clearIdentityState()
                        principal = null
                    }
                }
            }
        }
        viewModelScope.launch {
            readingStates.collect { states ->
                mutable.update { current ->
                    val selected = current.selectedHomework
                    val progress = selected?.let { states[it.readingKey(current.selectedBaseUrl)] }
                        ?.takeIf { it.phase == HomeworkReadPhase.DOWNLOADING || it.phase == HomeworkReadPhase.PROCESSING }?.progress
                    current.copy(progress = progress)
                }
            }
        }
        authWork = viewModelScope.launch { access.restore() }
    }
    private fun clearIdentityState() {
        identityVersion++; listVersion++; selectionVersion++
        work?.cancel(); resourceWork?.cancel(); readings.reset()
        exportDocument = null
        mutable.value = HomeworkUiState()
    }
    fun login(phone: String, password: String, remember: Boolean) {
        if (identity.value is AccessUiState.Working && enteredPhone == phone && enteredPassword == password) return
        enteredPhone = phone; enteredPassword = password
        authWork?.cancel()
        authWork = viewModelScope.launch { access.login(phone, password, remember) }
    }
    fun retrySavedSession() { authWork?.cancel(); authWork = viewModelScope.launch { access.restore() } }
    fun bindDevice() { authWork = viewModelScope.launch { access.bindDevice() } }
    fun cancelBinding() { authWork?.cancel(); access.cancelBinding() }
    fun challenge(proof: ChallengeProof, requestId: Long) { authWork = viewModelScope.launch { access.submitChallenge(proof, requestId) } }
    fun rejectChallenge(event: ChallengeEvent.Rejected, requestId: Long) = access.verificationRejected(event.failure, event.expired, requestId)
    fun retryChallenge() = access.retryVerification()
    fun cancelChallenge() { authWork?.cancel(); access.cancelChallenge() }
    fun selectAccount(key: String) = access.selectAccount(key)
    fun query(value: String) { mutable.update { it.copy(query = value) } }

    fun refresh(period: HomeworkPeriod = mutable.value.period) {
        if (mutable.value.clearingCache || work?.isActive == true && mutable.value.period == period) return
        work?.cancel()
        val request = ++listVersion
        val owner = identityVersion
        work = viewModelScope.launch {
            mutable.update { it.copy(period = period, batch = if (it.period == period) it.batch else HomeworkBatch(), reader = null, loading = true, feedback = null) }
            try {
                val batch = access.homework(period)
                if (owner == identityVersion && request == listVersion && batch != null) mutable.update { it.copy(batch = batch) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (owner == identityVersion && request == listVersion) mutable.update { it.copy(feedback = AppFeedback.Error(FailureClassifier.classify(e, FeedbackOperation.HOMEWORK))) } }
            finally { if (owner == identityVersion && request == listVersion) mutable.update { it.copy(loading = false) } }
        }
    }

    fun open(entry: HomeworkEntry) {
        if (identity.value !is AccessUiState.SignedIn || mutable.value.clearingCache) return
        val current = mutable.value
        val base = current.batch.baseUrl
        if (resourceWork?.isActive == true && current.selectedHomework?.readingKey(current.selectedBaseUrl) == entry.readingKey(base)) return
        resourceWork?.cancel()
        current.selectedHomework?.let { if (current.progress != null) readings.cancel(it, current.selectedBaseUrl) }
        val selection = ++selectionVersion
        val owner = identityVersion
        mutable.update { it.copy(selectedHomework = entry, selectedBaseUrl = base, reader = null, feedback = null, readerRetrying = false,
            progress = ResourceProgress(getApplication<Application>().getString(R.string.cache_checking, entry.name), 0f, HomeworkReadPhase.PROCESSING)) }
        resourceWork = viewModelScope.launch {
            try {
                val document = readings.read(entry, base)
                if (owner == identityVersion && selection == selectionVersion) mutable.update { it.copy(reader = document, progress = null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (owner == identityVersion && selection == selectionVersion) mutable.update { it.copy(feedback = AppFeedback.Error(FailureClassifier.classify(e, FeedbackOperation.DOWNLOAD)), progress = null) } }
        }
    }
    fun retrySelectedHomework() {
        val current = mutable.value
        val entry = current.selectedHomework ?: return
        if (identity.value !is AccessUiState.SignedIn || current.clearingCache || resourceWork?.isActive == true || current.reader?.isComplete == true) return
        val selection = ++selectionVersion; val owner = identityVersion
        mutable.update { it.copy(readerRetrying = true, feedback = null) }
        resourceWork = viewModelScope.launch {
            try {
                val document = readings.read(entry, current.selectedBaseUrl, retryFailed = true)
                if (owner == identityVersion && selection == selectionVersion) mutable.update { it.copy(reader = document, progress = null, readerRetrying = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (owner == identityVersion && selection == selectionVersion) mutable.update { it.copy(feedback = AppFeedback.Error(FailureClassifier.classify(e, FeedbackOperation.DOWNLOAD)), progress = null) }
            } finally { if (owner == identityVersion && selection == selectionVersion) mutable.update { it.copy(readerRetrying = false) } }
        }
    }
    fun closeReader() {
        if (mutable.value.readerRetrying) {
            selectionVersion++; resourceWork?.cancel()
            mutable.value.selectedHomework?.let { readings.cancel(it, mutable.value.selectedBaseUrl) }
        }
        mutable.update { it.copy(reader = null, progress = null, readerRetrying = false) }
    }

    fun clearCache() {
        if (cacheWork?.isActive == true) return
        val owner = identityVersion
        selectionVersion++; resourceWork?.cancel(); readings.reset()
        mutable.update { it.copy(reader = null, progress = null, clearingCache = true, feedback = null, readerRetrying = false) }
        cacheWork = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { cache.clear() }
                if (owner == identityVersion) mutable.update { it.copy(feedback = AppFeedback.Notice(NoticeKind.CACHE_CLEARED), selectedHomework = null, clearingCache = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (owner == identityVersion) mutable.update { it.copy(feedback = AppFeedback.Error(FailureClassifier.classify(e, FeedbackOperation.CACHE)), clearingCache = false) } }
        }
    }
    fun logout() {
        authWork?.cancel(); cacheWork?.cancel()
        enteredPhone = ""; enteredPassword = ""
        clearIdentityState(); access.logout()
    }
    fun cycleTheme() {
        val next = when (theme.value) { "system" -> "dark"; "dark" -> "light"; else -> "system" }
        store.theme = next; mutableTheme.value = next
    }
    fun export(uri: Uri?) {
        val document = exportDocument
        exportDocument = null
        if (uri == null || document == null) return
        val owner = identityVersion
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri) ?: error(getApplication<Application>().getString(R.string.fallback_export_open_error))
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(AnswerExport.text(document)) }
                }
                if (owner == identityVersion) mutable.update { it.copy(feedback = AppFeedback.Notice(NoticeKind.EXPORTED)) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (owner == identityVersion) mutable.update { it.copy(feedback = AppFeedback.Error(FailureClassifier.classify(e, FeedbackOperation.EXPORT))) } }
        }
    }
}
