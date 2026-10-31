package com.etsviewer.core.resources

import com.etsviewer.core.homework.ResourceChecksum
import com.etsviewer.core.service.ProtocolFailure
import com.etsviewer.core.feedback.DiagnosticKind
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Expected digests come only from explicit metadata in the authenticated homework response. */
internal object ResourceIntegrity {
    fun verify(file: File, checksums: List<ResourceChecksum>) {
        if (checksums.isEmpty()) return
        val digests = checksums.map { checksum ->
            val algorithm = when (checksum.algorithm.uppercase(Locale.ROOT).replace("-", "")) {
                "SHA256" -> "SHA-256"
                "SHA1" -> "SHA-1"
                "MD5" -> "MD5"
                else -> throw ProtocolFailure("Unsupported resource checksum algorithm", DiagnosticKind.CHECKSUM)
            }
            val digest = MessageDigest.getInstance(algorithm)
            if (checksum.value.length != digest.digestLength * 2 || !checksum.value.all { it in "0123456789abcdefABCDEF" })
                throw ProtocolFailure("Invalid resource checksum metadata", DiagnosticKind.CHECKSUM)
            digest to checksum.value
        }
        if (!file.isFile) throw ProtocolFailure("Missing archive for resource checksum validation", DiagnosticKind.CHECKSUM)
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digests.forEach { (digest, _) -> digest.update(buffer, 0, read) }
            }
        }
        for ((digest, expected) in digests) {
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(expected, ignoreCase = true)) throw ProtocolFailure("Resource checksum mismatch (${digest.algorithm})", DiagnosticKind.CHECKSUM)
        }
    }
}
