package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.QuestionDefinition
import com.etsviewer.core.service.string
import kotlinx.serialization.json.JsonObject

class RetellParser : QuestionTypeParser {
    override val supportedTypes = setOf("collector.picture")
    override fun parse(info: JsonObject, context: ParseContext) = ParseFields.content(info,
        listOf(QuestionDefinition(0, answers = ParseFields.variants(info), topic = TextNormalizer.clean(info.string("topic")),
            originalText = TextNormalizer.clean(info.string("value")))), originalText = "")
}

