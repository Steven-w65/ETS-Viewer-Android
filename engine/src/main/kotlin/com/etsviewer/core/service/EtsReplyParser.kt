package com.etsviewer.core.service

import kotlinx.serialization.json.*
import java.io.IOException

class ProtocolFailure(message: String) : IOException(message)
class ServiceFailure(val code: Int, message: String) : IOException(message)
class HttpFailure(val status: Int) : IOException("HTTP $status")
fun JsonObject.string(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
fun JsonObject.obj(key: String): JsonObject = get(key) as? JsonObject ?: JsonObject(emptyMap())
fun JsonObject.array(key: String): JsonArray = get(key) as? JsonArray ?: JsonArray(emptyList())

data class EtsReply(val code: Int?, val message: String, val body: JsonObject) {
    fun requireSuccess() {
        if (code != null && code != 0) throw ServiceFailure(code, message.ifBlank { "ETS100 error $code" })
    }
    fun requireToken(): String {
        requireSuccess()
        return body.string("token").takeIf { it.isNotBlank() } ?: throw ProtocolFailure("ETS100 did not return a session Token")
    }
    val needsChallenge: Boolean get() = code != null && code != 0 && listOf("验证", "校验", "尝试", "太多", "captcha", "verification").any { message.contains(it, ignoreCase = true) }
}

object EtsReplyParser {
    fun parse(text: String): EtsReply {
        val value = try { Json.parseToJsonElement(text) } catch (_: Exception) { throw ProtocolFailure("ETS100 returned invalid JSON") }
        val normalized = if (value is JsonArray) value.firstOrNull() else value
        val root = normalized as? JsonObject ?: throw ProtocolFailure("ETS100 response must be a non-empty object or array")
        if ("code" in root && root["code"] !is JsonNull && root.string("code").toIntOrNull() == null) throw ProtocolFailure("ETS100 returned an invalid business code")
        return EtsReply(root.string("code").toIntOrNull(), root.string("msg"), root.obj("body"))
    }
}
