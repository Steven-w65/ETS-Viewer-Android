package com.etsviewer.core.feedback

import com.etsviewer.core.service.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import net.lingala.zip4j.exception.ZipException
import java.io.IOException
import java.net.*
import javax.net.ssl.*

/** Classifies once where an operation fails; the UI never guesses from exception text. */
object FailureClassifier {
    fun classify(error: Throwable, operation: FeedbackOperation): AppFailure {
        val causes = generateSequence(error) { it.cause }.take(16).toList()
        causes.filterIsInstance<CancellationException>().firstOrNull()?.let { throw it }
        val http = causes.filterIsInstance<HttpFailure>().firstOrNull()?.status?.takeIf { it in 100..599 }
        val service = causes.filterIsInstance<ServiceFailure>().firstOrNull()
        val network = causes.any { it is UnknownHostException || it is ConnectException || it is SocketTimeoutException || it is SSLException || it is ProtocolException }
        val diagnostics = buildSet {
            causes.filterIsInstance<ProtocolFailure>().mapNotNullTo(this) { it.diagnostic }
            if (causes.any { it is ZipException }) add(DiagnosticKind.ARCHIVE)
            if (causes.filterIsInstance<ZipException>().any { it.type == ZipException.Type.CHECKSUM_MISMATCH }) add(DiagnosticKind.CRC)
            if (causes.any { it is SSLPeerUnverifiedException }) add(DiagnosticKind.HOSTNAME)
            else if (causes.any { it is SSLException }) add(DiagnosticKind.TLS)
        }
        val kind = when {
            causes.any { it is MissingIntegration } -> FailureKind.CONFIGURATION
            service?.issue == ServiceIssue.INVALID_PASSWORD -> FailureKind.INVALID_PASSWORD
            service?.issue == ServiceIssue.INVALID_PHONE -> FailureKind.INVALID_PHONE
            network -> FailureKind.NETWORK
            operation == FeedbackOperation.DOWNLOAD && http in listOf(403, 404) -> FailureKind.RESOURCE_ACCESS
            operation == FeedbackOperation.DOWNLOAD && causes.any { it is ProtocolFailure || it is ZipException || it is SerializationException } -> FailureKind.RESOURCE
            else -> FailureKind.UNKNOWN
        }
        val type = causes.firstNotNullOfOrNull { cause -> when (cause) {
            is UnknownHostException -> FailureException.UnknownHostException
            is ConnectException -> FailureException.ConnectException
            is SocketTimeoutException -> FailureException.SocketTimeoutException
            is SSLPeerUnverifiedException -> FailureException.SSLPeerUnverifiedException
            is SSLHandshakeException -> FailureException.SSLHandshakeException
            is SSLException -> FailureException.SSLException
            is ProtocolException -> FailureException.ProtocolException
            is ProtocolFailure -> FailureException.ProtocolFailure
            is ZipException -> FailureException.ZipException
            is SerializationException -> FailureException.SerializationException
            // Prefer a specific nested cause to an IOException wrapper.
            else -> null
        } } ?: FailureException.IOException.takeIf { causes.any { it is IOException } }
        return AppFailure(operation, kind, diagnostics, http, type)
    }
}
