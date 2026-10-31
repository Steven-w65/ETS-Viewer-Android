package com.etsviewer.android.ui

import com.etsviewer.android.R
import com.etsviewer.core.feedback.*
import com.etsviewer.core.identity.AccessWork

typealias ErrorContext = FeedbackOperation
enum class FormField { PHONE, PASSWORD }
data class UiError(val title: String, val message: String, val explanation: String, val details: String, val field: FormField? = null)
data class LoginValidation(val phone: String? = null, val password: String? = null) {
    val isValid: Boolean get() = phone == null && password == null
}

/** Presentation only: arbitrary server/exception text is never rendered, even in the details dialog. */
class FeedbackPresenter(private val texts: UiStrings) {
    fun validateLogin(phone: String, password: String) = LoginValidation(
        when { phone.isBlank() -> texts.text(R.string.validation_phone_required); !phone.trim().matches(Regex("1[3-9]\\d{9}")) -> texts.text(R.string.validation_phone_invalid); else -> null },
        if (password.isEmpty()) texts.text(R.string.validation_password_required) else null,
    )
    fun notice(kind: NoticeKind): String = texts.text(when (kind) {
        NoticeKind.CACHE_CLEARED -> R.string.notice_cache_cleared
        NoticeKind.EXPORTED -> R.string.notice_exported
        NoticeKind.EXPORT_CANCELLED -> R.string.notice_export_cancelled
        NoticeKind.VERIFICATION_CANCELLED -> R.string.notice_verification_cancelled
        NoticeKind.BINDING_CANCELLED -> R.string.notice_binding_cancelled
        NoticeKind.CANCELLED -> R.string.notice_cancelled
        NoticeKind.LOGGED_OUT -> R.string.notice_logged_out
    })
    fun working(stage: AccessWork): String = texts.text(when (stage) {
        AccessWork.RESTORE -> R.string.working_restore
        AccessWork.ACCOUNTS -> R.string.working_accounts
        AccessWork.BINDING -> R.string.working_binding
        AccessWork.RENEW -> R.string.working_renew
        AccessWork.LOGIN, AccessWork.VERIFIED_LOGIN -> R.string.working_login
    })
    fun error(failure: AppFailure): UiError {
        val context = failure.operation
        val field = if (context == ErrorContext.LOGIN) when (failure.kind) {
            FailureKind.INVALID_PASSWORD -> FormField.PASSWORD
            FailureKind.INVALID_PHONE -> FormField.PHONE
            else -> null
        } else null
        val title = when (context) {
            ErrorContext.LOGIN -> texts.text(R.string.feedback_login_title)
            ErrorContext.HOMEWORK -> texts.text(R.string.feedback_homework_title)
            ErrorContext.DOWNLOAD -> texts.text(R.string.feedback_download_title)
            ErrorContext.VERIFICATION -> texts.text(R.string.feedback_verification_title)
            ErrorContext.EXPORT -> texts.text(R.string.feedback_export_title)
            ErrorContext.MEDIA -> texts.text(R.string.feedback_media_title)
            ErrorContext.SESSION -> texts.text(R.string.feedback_session_title)
            ErrorContext.CACHE -> texts.text(R.string.feedback_cache_title)
        }
        val message = when {
            context == ErrorContext.SESSION -> texts.text(R.string.feedback_session_body)
            context == ErrorContext.LOGIN && failure.kind == FailureKind.CONFIGURATION -> texts.text(R.string.feedback_configuration_body)
            field == FormField.PASSWORD -> texts.text(R.string.feedback_password_body)
            field == FormField.PHONE -> texts.text(R.string.feedback_phone_body)
            failure.kind == FailureKind.NETWORK -> texts.text(R.string.feedback_network_body)
            context == ErrorContext.DOWNLOAD && failure.kind == FailureKind.RESOURCE -> texts.text(R.string.feedback_resource_body)
            context == ErrorContext.DOWNLOAD && failure.kind == FailureKind.RESOURCE_ACCESS -> texts.text(R.string.feedback_access_body)
            context == ErrorContext.VERIFICATION -> texts.text(R.string.feedback_verification_body)
            context == ErrorContext.EXPORT -> texts.text(R.string.feedback_export_body)
            context == ErrorContext.MEDIA -> texts.text(R.string.feedback_media_body)
            context == ErrorContext.CACHE -> texts.text(R.string.feedback_cache_body)
            context == ErrorContext.LOGIN -> texts.text(R.string.feedback_login_body)
            context == ErrorContext.DOWNLOAD -> texts.text(R.string.feedback_download_body)
            else -> texts.text(R.string.feedback_homework_body)
        }
        val explanation = when (context) {
            ErrorContext.DOWNLOAD -> texts.text(R.string.feedback_download_explanation)
            ErrorContext.SESSION -> texts.text(R.string.feedback_session_explanation)
            ErrorContext.VERIFICATION -> texts.text(R.string.feedback_verification_explanation)
            ErrorContext.EXPORT -> texts.text(R.string.feedback_export_explanation)
            else -> texts.text(R.string.feedback_generic_explanation)
        }
        return UiError(title, message, explanation, details(listOf(failure)), field)
    }

    fun details(failures: List<AppFailure>): String {
        val facts = mutableListOf<String>()
        failures.mapNotNull { it.exception }.distinct().sortedBy { it.ordinal }.forEach {
            facts += texts.text(R.string.diagnostic_type, it.name)
        }
        failures.mapNotNull { it.httpStatus }.filter { it in 100..599 }.distinct().sorted().forEach {
            facts += texts.text(R.string.diagnostic_http, it)
        }
        failures.flatMap { it.diagnostics }.distinct().sortedBy { it.ordinal }.forEach { kind ->
            facts += texts.text(when (kind) {
                DiagnosticKind.CHECKSUM -> R.string.diagnostic_checksum
                DiagnosticKind.CRC -> R.string.diagnostic_crc
                DiagnosticKind.ARCHIVE -> R.string.diagnostic_archive
                DiagnosticKind.CONTENT -> R.string.diagnostic_content
                DiagnosticKind.HOSTNAME -> R.string.diagnostic_hostname
                DiagnosticKind.TLS -> R.string.diagnostic_tls
            })
        }
        return facts.joinToString("\n").ifBlank { texts.text(R.string.diagnostic_empty) }
    }
}
