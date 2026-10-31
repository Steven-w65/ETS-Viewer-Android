package com.etsviewer.core.feedback

import java.util.concurrent.atomic.AtomicLong

enum class FeedbackOperation { LOGIN, HOMEWORK, DOWNLOAD, VERIFICATION, EXPORT, MEDIA, SESSION, CACHE }
enum class FailureKind { UNKNOWN, NETWORK, CONFIGURATION, INVALID_PHONE, INVALID_PASSWORD, RESOURCE, RESOURCE_ACCESS }
enum class DiagnosticKind { CHECKSUM, CRC, ARCHIVE, CONTENT, HOSTNAME, TLS }
enum class FailureException {
    UnknownHostException, ConnectException, SocketTimeoutException, SSLPeerUnverifiedException,
    SSLHandshakeException, SSLException, ProtocolException, ProtocolFailure, ZipException, SerializationException, IOException,
}

/** Safe facts only: never carries a server body, exception message, URL or credentials. */
data class AppFailure(
    val operation: FeedbackOperation,
    val kind: FailureKind = FailureKind.UNKNOWN,
    val diagnostics: Set<DiagnosticKind> = emptySet(),
    val httpStatus: Int? = null,
    val exception: FailureException? = null,
)

enum class NoticeKind { CACHE_CLEARED, EXPORTED, EXPORT_CANCELLED, VERIFICATION_CANCELLED, BINDING_CANCELLED, CANCELLED, LOGGED_OUT }
private val feedbackIds = AtomicLong()

/** Identity belongs to an occurrence, not its localized text. */
sealed interface AppFeedback {
    val id: Long
    data class Error(val failure: AppFailure, override val id: Long = feedbackIds.incrementAndGet()) : AppFeedback
    data class Notice(val kind: NoticeKind, override val id: Long = feedbackIds.incrementAndGet()) : AppFeedback
}
