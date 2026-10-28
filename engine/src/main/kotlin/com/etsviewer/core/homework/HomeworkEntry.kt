package com.etsviewer.core.homework

import com.etsviewer.core.service.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

enum class HomeworkPeriod(val wireValue: String) { CURRENT("1"), HISTORY("2") }
@Serializable data class ResourceChecksum(val algorithm: String, val value: String)
@Serializable data class ResourceGroup(val name: String, val url: String, val checksums: List<ResourceChecksum> = emptyList())
@Serializable data class HomeworkEntry(val id: String, val name: String, val resources: List<ResourceGroup>) {
    fun readingKey(baseUrl: String): String = digest("SHA-256", buildJsonObject {
        put("base", baseUrl); put("id", id); put("name", name)
        putJsonArray("resources") { resources.forEach { item -> add(buildJsonObject {
            put("name", item.name); put("url", item.url)
            if (item.checksums.isNotEmpty()) putJsonArray("checksums") { item.checksums.forEach { checksum ->
                add(buildJsonObject { put("algorithm", checksum.algorithm); put("value", checksum.value) })
            } }
        }) } }
    }.toString().toByteArray(Charsets.UTF_8))
}
data class HomeworkBatch(val baseUrl: String = "https://cdn.subject.ets100.com", val entries: List<HomeworkEntry> = emptyList()) {
    fun search(query: String) = entries.filter { it.name.contains(query.trim(), ignoreCase = true) }
}

object HomeworkSource {
    fun decode(reply: EtsReply): HomeworkBatch {
        reply.requireSuccess()
        if (reply.code == -1) throw ServiceFailure(-1, reply.message.ifBlank { "Homework request was rejected" })
        val body = reply.body
        if (body["data"] !is JsonArray) throw ProtocolFailure("Homework response is missing body.data")
        return HomeworkBatch(body.string("base_url").ifBlank { "https://cdn.subject.ets100.com" }, body.array("data").map { raw ->
            val item = raw as? JsonObject ?: throw ProtocolFailure("Invalid homework entry")
            HomeworkEntry(item.string("id"), item.string("name").ifBlank { "未知作业" }, item.obj("struct").array("contents").map { value ->
                val group = value as? JsonObject ?: throw ProtocolFailure("Invalid homework resource")
                ResourceGroup(group.string("group_name"), group.string("url"), checksums(group))
            })
        })
    }

    private fun checksums(group: JsonObject): List<ResourceChecksum> = buildList {
        for ((field, algorithm) in listOf("sha256" to "SHA-256", "sha1" to "SHA-1", "md5" to "MD5")) {
            if (field in group) add(ResourceChecksum(algorithm, (group[field] as? JsonPrimitive)?.contentOrNull.orEmpty()))
        }
        if ("checksum" in group || "checksum_algorithm" in group) {
            val checksum = group["checksum"]
            if (checksum is JsonObject) add(ResourceChecksum(checksum.string("algorithm"), checksum.string("value")))
            else add(ResourceChecksum(group.string("checksum_algorithm"), (checksum as? JsonPrimitive)?.contentOrNull.orEmpty()))
        }
    }
}
