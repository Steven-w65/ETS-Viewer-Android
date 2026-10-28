package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.QuestionDefinition
import com.etsviewer.core.service.string
import kotlinx.serialization.json.JsonObject

class ReadingParser : QuestionTypeParser {
    override val supportedTypes = setOf("collector.word", "collector.read")
    override fun parse(info: JsonObject, context: ParseContext) = ParseFields.content(info,
        listOf(QuestionDefinition(0, number = info.string("xh").ifBlank { "1" }, originalText = TextNormalizer.clean(info.string("value")),
            explanation = ParseFields.explanation(info).ifBlank { TextNormalizer.clean(info.string("analyze")) },
            translation = TextNormalizer.clean(info.string("translate")))),
        originalText = "", explanation = "", allowsSectionMediaOnly = true)
}

