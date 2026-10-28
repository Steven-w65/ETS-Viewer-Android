package com.etsviewer.core.identity

import com.etsviewer.core.accounts.LearnerAccount

sealed interface AccessUiState {
    data class SignedOut(val message: String = "", val phone: String = "") : AccessUiState
    data class Working(val message: String) : AccessUiState
    data class VerificationRequired(val phone: String, val requestId: Long = 0, val message: String = "", val expired: Boolean = false) : AccessUiState
    data class BindingRequired(val message: String, val phone: String) : AccessUiState
    data class ChooseAccount(val accounts: List<LearnerAccount>) : AccessUiState
    data class SignedIn(val session: LoginSession) : AccessUiState
    data class Failure(val message: String, val phone: String) : AccessUiState
}
