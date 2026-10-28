package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.QuestionDefinition
import com.etsviewer.core.service.*
import kotlinx.serialization.json.*

internal object ParseFields {
    fun variants(value: JsonObject) = value.array("std").mapNotNull { item ->
        (item as? JsonObject)?.string("value")?.takeIf(String::isNotEmpty)?.let(TextNormalizer::clean)
    }
    fun explanation(value: JsonObject) = listOf("explanation", "analysis", "explain", "jx")
        .firstNotNullOfOrNull { value.string(it).takeIf(String::isNotBlank)?.let(TextNormalizer::clean) }.orEmpty()

    fun content(info: JsonObject, questions: List<QuestionDefinition>,
        originalText: String = TextNormalizer.clean(info.string("value")),
        explanation: String = explanation(info), allowsSectionMediaOnly: Boolean = false): ParsedSectionContent {
        val common = JsonObject(info.filterKeys { it !in setOf("question", "xtlist", "std") })
        return ParsedSectionContent(questions, MediaExtractor.collect(common), originalText, explanation, allowsSectionMediaOnly)
    }
}

