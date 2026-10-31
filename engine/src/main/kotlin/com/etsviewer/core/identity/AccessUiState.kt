package com.etsviewer.core.identity

import com.etsviewer.core.accounts.LearnerAccount
import com.etsviewer.core.feedback.*

enum class AccessWork { LOGIN, RESTORE, ACCOUNTS, BINDING, RENEW, VERIFIED_LOGIN }

sealed interface AccessUiState {
    data class SignedOut(val notice: AppFeedback.Notice? = null, val phone: String = "") : AccessUiState
    data class Working(val stage: AccessWork) : AccessUiState
    data class VerificationRequired(val phone: String, val requestId: Long = 0, val failure: AppFailure? = null, val expired: Boolean = false) : AccessUiState
    data class BindingRequired(val phone: String) : AccessUiState
    data class ChooseAccount(val accounts: List<LearnerAccount>) : AccessUiState
    data class SignedIn(val session: LoginSession) : AccessUiState
    data class Failure(val failure: AppFailure, val phone: String) : AccessUiState
}
