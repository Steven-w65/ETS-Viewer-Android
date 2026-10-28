package com.etsviewer.core.answers.parsing

/** Shared historical text cleanup, independent of question type and UI. */
object TextNormalizer {
    fun clean(text: String): String = text.replace(Regex("ets_th\\d+\\s*"), "")
        .replace(Regex("</p>\\s*<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<br\\s*/?>|</br>|</p>|<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "").let(::decodeEntities).replace(Regex("[\u200b\ufeff\u2060]"), "")
        .lines().map(String::trim).filter(String::isNotEmpty).joinToString("\n")

    private fun decodeEntities(text: String): String = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos|nbsp);").replace(text) { match ->
        val entity = match.groupValues[1]
        when (entity) {
            "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> " "
            else -> runCatching {
                val code = if (entity.startsWith("#x")) entity.drop(2).toInt(16) else entity.drop(1).toInt()
                require(Character.isValidCodePoint(code))
                String(Character.toChars(code))
            }.getOrDefault(match.value)
        }
    }
}

