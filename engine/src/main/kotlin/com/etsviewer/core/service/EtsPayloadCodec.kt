package com.etsviewer.core.service

import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.Base64

class MissingIntegration(message: String) : IllegalStateException(message)

interface RequestSigner {
    val pid: String
    fun sign(timestamp: Long, encodedBody: String): String
}

class Md5RequestSigner(override val pid: String, private val secret: String) : RequestSigner {
    override fun sign(timestamp: Long, encodedBody: String): String {
        val missing = buildList {
            if (pid.isBlank()) add("ets.pid")
            if (secret.isBlank()) add("ets.signingSecret")
        }
        if (missing.isNotEmpty()) throw MissingIntegration("Missing ETS100 service-issued configuration for this application: " + missing.joinToString(", "))
        return digest("MD5", (pid + timestamp + encodedBody + secret).toByteArray(Charsets.UTF_8))
    }
}

internal fun digest(algorithm: String, bytes: ByteArray): String =
    MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) }

class EtsPayloadCodec(private val signer: RequestSigner, private val timeSeconds: () -> Long = { System.currentTimeMillis() / 1000 }) {
    fun encode(route: String, parameters: JsonObject): String {
        val business = buildJsonArray { add(buildJsonObject { put("r", route); put("params", parameters) }) }
        val encoded = Base64.getEncoder().encodeToString(business.toString().toByteArray(Charsets.UTF_8))
        val time = timeSeconds()
        val sign = signer.sign(time, encoded)
        return buildJsonObject {
            put("body", encoded)
            putJsonObject("head") { put("version", "1.0"); put("sign", sign); put("pid", signer.pid); put("time", time) }
        }.toString()
    }
}
