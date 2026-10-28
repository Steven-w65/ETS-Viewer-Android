package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.QuestionDefinition
import com.etsviewer.core.service.*
import kotlinx.serialization.json.JsonObject

class FillParser : QuestionTypeParser {
    override val supportedTypes = setOf("collector.fill")
    override fun parse(info: JsonObject, context: ParseContext): ParsedSectionContent {
        val questions = info.array("std").mapIndexed { index, raw ->
            val q = raw as? JsonObject ?: throw ProtocolFailure("Invalid fill answer at $index")
            QuestionDefinition(index, q.string("xth").ifBlank { (index + 1).toString() }, answers = listOf(TextNormalizer.clean(q.string("value"))),
                media = MediaExtractor.collect(q), explanation = ParseFields.explanation(q))
        }
        return ParseFields.content(info, questions)
    }
}

