package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.*
import kotlinx.serialization.json.*

/** Preserve ordered media references before text cleanup removes markup. */
object MediaExtractor {
    fun collect(element: JsonElement): List<MediaReference> {
        val found = mutableListOf<MediaReference>()
        fun visit(value: JsonElement) {
            when (value) {
                is JsonObject -> value.forEach { (_, v) -> visit(v) }
                is JsonArray -> value.forEach { visit(it) }
                is JsonPrimitive -> {
                    val text = value.contentOrNull ?: return
                    Regex("""<(img|audio|video|source)\b[^>]*?\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).findAll(text).forEach { match ->
                        val tag = match.groupValues[1].lowercase()
                        found += MediaReference(if (tag == "img") MediaKind.IMAGE else if (tag == "video") MediaKind.VIDEO else MediaKind.AUDIO, match.groupValues[2])
                    }
                    val path = text.substringBefore('?').lowercase()
                    val kind = when {
                        path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".webp") || path.endsWith(".gif") -> MediaKind.IMAGE
                        path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".m4a") || path.endsWith(".ogg") -> MediaKind.AUDIO
                        path.endsWith(".mp4") || path.endsWith(".webm") -> MediaKind.VIDEO
                        else -> null
                    }
                    if (kind != null && !text.contains('<') && !text.contains('\n')) found += MediaReference(kind, text)
                }
            }
        }
        visit(element)
        return found.distinct()
    }
}

