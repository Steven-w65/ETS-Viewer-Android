package com.etsviewer.android.ui

import com.etsviewer.android.R

enum class ErrorContext { LOGIN, HOMEWORK, DOWNLOAD, VERIFICATION, EXPORT, MEDIA, SESSION, CACHE }
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
    fun notice(message: String): String? {
        // Legacy controller status tokens are inputs, not visible localized copy.
        val resources = linkedMapOf("资源缓存已清理" to R.string.notice_cache_cleared, "答案已导出" to R.string.notice_exported,
            "已取消导出" to R.string.notice_export_cancelled, "已取消验证" to R.string.notice_verification_cancelled,
            "已取消设备绑定" to R.string.notice_binding_cancelled, "已取消操作" to R.string.notice_cancelled, "已退出登录" to R.string.notice_logged_out)
        val id = resources[message] ?: resources.values.firstOrNull { texts.text(it) == message }
        return id?.let { texts.text(it) }
    }
    fun working(message: String): String = when {
        message.contains("恢复") -> texts.text(R.string.working_restore)
        message.contains("账户") -> texts.text(R.string.working_accounts)
        message.contains("绑定") -> texts.text(R.string.working_binding)
        message.contains("更新") -> texts.text(R.string.working_renew)
        else -> texts.text(R.string.working_login)
    }
    fun error(raw: String, context: ErrorContext): UiError {
        val source = raw.take(64_000)
        fun has(vararg values: String) = values.any { source.contains(it, ignoreCase = true) }
        val field = if (context == ErrorContext.LOGIN) when {
            has("密码错误", "密码不正确", "incorrect password", "wrong password", "手机号或密码错误") -> FormField.PASSWORD
            has("手机号格式", "invalid phone") -> FormField.PHONE
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
            context == ErrorContext.LOGIN && has("Missing ETS100", "MissingIntegration") -> texts.text(R.string.feedback_configuration_body)
            field == FormField.PASSWORD -> texts.text(R.string.feedback_password_body)
            field == FormField.PHONE -> texts.text(R.string.feedback_phone_body)
            has("UnknownHost", "ConnectException", "timeout", "timed out", "SSL", "Hostname", "Unable to resolve", "网络") -> texts.text(R.string.feedback_network_body)
            context == ErrorContext.DOWNLOAD && has("ZIP", "archive", "CRC", "checksum", "content.json", "parsing", "structure", "Incomplete", "Empty resource") -> texts.text(R.string.feedback_resource_body)
            context == ErrorContext.DOWNLOAD && has("403", "404") -> texts.text(R.string.feedback_access_body)
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
        // Allowlist facts instead of attempting to redact arbitrary credentials, URLs or response bodies.
        val facts = mutableListOf<String>()
        val types = listOf("UnknownHostException", "ConnectException", "SocketTimeoutException", "SSLPeerUnverifiedException", "SSLHandshakeException", "SSLException", "ProtocolException", "ProtocolFailure", "ZipException", "SerializationException", "IOException")
        types.firstOrNull { has(it) }?.let { facts += texts.text(R.string.diagnostic_type, it) }
        Regex("\\bHTTP(?:\\s+(?:error|status))?\\s*[:=]?\\s*(\\d{3})\\b", RegexOption.IGNORE_CASE)
            .find(source)?.groupValues?.get(1)?.takeIf { it.toInt() in 100..599 }?.let { facts += texts.text(R.string.diagnostic_http, it) }
        when {
            has("checksum") -> facts += texts.text(R.string.diagnostic_checksum)
            has("CRC") -> facts += texts.text(R.string.diagnostic_crc)
            has("ZIP", "archive") -> facts += texts.text(R.string.diagnostic_archive)
            has("content.json", "parsing", "structure") -> facts += texts.text(R.string.diagnostic_content)
        }
        if (has("Hostname")) facts += texts.text(R.string.diagnostic_hostname)
        else if (has("SSL", "证书")) facts += texts.text(R.string.diagnostic_tls)
        return UiError(title, message, explanation, facts.joinToString("\n").ifBlank { texts.text(R.string.diagnostic_empty) }, field)
    }
}
