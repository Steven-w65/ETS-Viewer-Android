package com.etsviewer.core.service

enum class ServiceIssue { UNKNOWN, INVALID_PHONE, INVALID_PASSWORD }

/** Legacy ETS replies lack stable documented codes for these fields. Keep compatibility here only. */
internal fun serviceIssue(message: String): ServiceIssue = when {
    listOf("密码错误", "密码不正确", "incorrect password", "wrong password", "手机号或密码错误").any { message.contains(it, ignoreCase = true) } -> ServiceIssue.INVALID_PASSWORD
    listOf("手机号格式", "invalid phone").any { message.contains(it, ignoreCase = true) } -> ServiceIssue.INVALID_PHONE
    else -> ServiceIssue.UNKNOWN
}
