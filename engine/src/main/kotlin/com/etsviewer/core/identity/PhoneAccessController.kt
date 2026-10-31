package com.etsviewer.core.identity

import com.etsviewer.core.accounts.*
import com.etsviewer.core.homework.*
import com.etsviewer.core.service.*
import com.etsviewer.core.feedback.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class PhoneAccessController(private val api: CloudApi, private val store: LoginSessionStore) {
    private val mutable = MutableStateFlow<AccessUiState>(AccessUiState.SignedOut())
    val state = mutable.asStateFlow()
    private val generation = AtomicLong()
    private data class Pending(
        val phone: String, val password: String?, val remember: Boolean, val generation: Long,
        val preferred: LearnerAccount? = null,
    )
    private var pending: Pending? = null
    private var accountToken: String? = null
    private var accountChoices: List<LearnerAccount> = emptyList()

    suspend fun login(phone: String, password: String, rememberPassword: Boolean) {
        if (state.value is AccessUiState.Working && pending?.let { it.phone == phone.trim() && it.password == password } == true) return
        val context = Pending(phone.trim(), password, rememberPassword, generation.incrementAndGet())
        pending = context; accountToken = null; accountChoices = emptyList()
        if (context.phone.isBlank() || password.isEmpty()) {
            mutable.value = AccessUiState.Failure(AppFailure(FeedbackOperation.LOGIN,
                if (context.phone.isBlank()) FailureKind.INVALID_PHONE else FailureKind.INVALID_PASSWORD), context.phone); return
        }
        run(context) {
            mutable.value = AccessUiState.Working(AccessWork.LOGIN)
            acceptLogin(api.login(context.phone, password, store.installationId()), context)
        }
    }

    suspend fun restore() {
        val id = generation.incrementAndGet()
        pending = null; accountToken = null; accountChoices = emptyList()
        val saved = store.read()
        if (saved == null || saved.token.isBlank() || saved.account.parentId.isBlank()) {
            mutable.value = AccessUiState.SignedOut(); return
        }
        val context = Pending(saved.phone, saved.password, saved.password != null, id, saved.account)
        pending = context
        run(context) {
            mutable.value = AccessUiState.Working(AccessWork.RESTORE)
            try { completeLogin(saved.token, context) }
            catch (e: Exception) {
                if (e is CancellationException || !sessionRejected(e)) throw e
                if (active(context)) renewSession(saved, context)
            }
        }
    }

    suspend fun bindDevice() {
        val context = pending ?: return
        if (state.value !is AccessUiState.BindingRequired || !active(context)) return
        run(context) {
            val password = context.password ?: throw ProtocolFailure("请重新输入密码后绑定设备")
            mutable.value = AccessUiState.Working(AccessWork.BINDING)
            acceptLogin(api.bindDevice(context.phone, password, store.installationId()), context)
        }
    }

    suspend fun submitChallenge(
        proof: ChallengeProof,
        requestId: Long = (state.value as? AccessUiState.VerificationRequired)?.requestId ?: -1,
    ) {
        val context = pending ?: return
        val challenge = state.value as? AccessUiState.VerificationRequired ?: return
        if (!active(context) || requestId != challenge.requestId || proof.uid.isBlank() || proof.captchaResult.isBlank()) return
        mutable.value = AccessUiState.Working(AccessWork.VERIFIED_LOGIN)
        run(context) {
            val reply = api.continueLogin(proof.uid, proof.captchaResult, store.installationId())
            if (reply.code != null && reply.code != 0 && reply.code != 30014)
                requestChallenge(context, AppFailure(FeedbackOperation.VERIFICATION), ChallengeCoordinator.expired(reply.message), fresh = true)
            else acceptLogin(reply, context)
        }
    }

    fun verificationRejected(failure: AppFailure, expired: Boolean, requestId: Long) {
        val context = pending ?: return
        val challenge = state.value as? AccessUiState.VerificationRequired ?: return
        if (active(context) && challenge.requestId == requestId)
            mutable.value = challenge.copy(failure = failure, expired = expired)
    }

    fun retryVerification() {
        val context = pending ?: return
        if (state.value is AccessUiState.VerificationRequired && active(context)) requestChallenge(context, fresh = true)
    }

    fun cancelChallenge() {
        cancelPending(NoticeKind.VERIFICATION_CANCELLED)
    }

    fun cancelBinding() { cancelPending(NoticeKind.BINDING_CANCELLED) }

    private fun cancelPending(notice: NoticeKind) {
        val phone = pending?.phone.orEmpty()
        generation.incrementAndGet(); clearPending()
        mutable.value = AccessUiState.SignedOut(AppFeedback.Notice(notice), phone)
    }

    fun selectAccount(key: String) {
        val context = pending ?: return
        val token = accountToken ?: return
        if (!active(context) || state.value !is AccessUiState.ChooseAccount) return
        val account = accountChoices.firstOrNull { it.key == key } ?: return
        save(token, account, context)
    }

    private fun requestChallenge(context: Pending, failure: AppFailure? = null, expired: Boolean = false, fresh: Boolean = false) {
        if (!active(context)) return
        val next = if (fresh) context.copy(generation = generation.incrementAndGet()) else context
        pending = next
        mutable.value = AccessUiState.VerificationRequired(next.phone, next.generation, failure, expired)
    }

    private suspend fun acceptLogin(reply: EtsReply, context: Pending) {
        if (!active(context)) return
        when {
            reply.code == 30014 -> mutable.value = AccessUiState.BindingRequired(context.phone)
            reply.needsChallenge -> requestChallenge(context, AppFailure(FeedbackOperation.VERIFICATION), ChallengeCoordinator.expired(reply.message))
            else -> completeLogin(reply.requireToken(), context)
        }
    }

    private suspend fun completeLogin(token: String, context: Pending) {
        if (!active(context)) return
        mutable.value = AccessUiState.Working(AccessWork.ACCOUNTS)
        val candidates = LearnerAccountSource.preferred(api.accounts(token))
        if (!active(context)) return
        if (candidates.isEmpty()) throw ProtocolFailure("未找到有效的账户信息 (parent_id / 班级已过期或不可用)")
        val matches = candidates.filter { context.preferred?.id?.takeIf(String::isNotBlank) == it.id }
        val previous = matches.firstOrNull { it.key == context.preferred?.key } ?: matches.singleOrNull()
            ?: candidates.firstOrNull { context.preferred?.id.isNullOrBlank() && it.key == context.preferred?.key && it.parentId == context.preferred?.parentId }
        if (previous != null) save(token, previous, context)
        else if (candidates.size == 1) save(token, candidates.single(), context)
        else {
            accountToken = token; accountChoices = candidates
            mutable.value = AccessUiState.ChooseAccount(candidates)
        }
    }

    private fun save(token: String, account: LearnerAccount, context: Pending) {
        if (!active(context)) return
        val session = LoginSession(context.phone, token, account, context.password.takeIf { context.remember })
        store.write(session); clearPending()
        mutable.value = AccessUiState.SignedIn(session)
    }
    private fun clearPending() { pending = null; accountToken = null; accountChoices = emptyList() }
    private fun active(context: Pending) = generation.get() == context.generation
    private fun sessionRejected(error: Exception): Boolean =
        error is ServiceFailure && error.code != 80004 || error is HttpFailure && error.status == 401
    private suspend fun run(context: Pending, work: suspend () -> Unit) {
        try { work() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (active(context)) {
                clearPending()
                mutable.value = AccessUiState.Failure(FailureClassifier.classify(e, FeedbackOperation.LOGIN), context.phone)
            }
        }
    }

    private suspend fun renewSession(session: LoginSession, context: Pending) {
        mutable.value = AccessUiState.Working(AccessWork.RENEW)
        try { acceptLogin(api.renew(session.token), context) }
        catch (e: Exception) {
            if (e is CancellationException || !sessionRejected(e)) throw e
            if (!active(context)) return
            val password = session.password
            if (password == null) {
                store.clearSession()
                throw e
            }
            acceptLogin(api.login(session.phone, password, store.installationId()), context)
        }
    }

    suspend fun homework(period: HomeworkPeriod): HomeworkBatch? {
        val session = (state.value as? AccessUiState.SignedIn)?.session ?: return null
        val id = generation.get()
        try {
            return api.homework(session.token, session.account.parentId, period).takeIf { generation.get() == id }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (generation.get() != id) return null
            if (!sessionRejected(e)) throw e
            val context = Pending(session.phone, session.password, session.password != null, generation.incrementAndGet(), session.account)
            pending = context
            run(context) { renewSession(session, context) }
            val renewed = state.value as? AccessUiState.SignedIn ?: return null
            val renewedId = generation.get()
            return try {
                api.homework(renewed.session.token, renewed.session.account.parentId, period).takeIf { generation.get() == renewedId }
            } catch (retry: Exception) {
                if (retry is CancellationException) throw retry
                if (!sessionRejected(retry)) throw retry
                if (generation.get() == renewedId) {
                    generation.incrementAndGet(); store.clearSession(); clearPending()
                    mutable.value = AccessUiState.Failure(FailureClassifier.classify(retry, FeedbackOperation.SESSION), renewed.session.phone)
                }
                null
            }
        }
    }

    fun logout() {
        generation.incrementAndGet(); clearPending()
        store.clearSession(); mutable.value = AccessUiState.SignedOut()
    }
}
