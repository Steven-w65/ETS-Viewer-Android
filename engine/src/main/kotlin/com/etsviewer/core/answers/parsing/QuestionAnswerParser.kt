package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.QuestionDefinition
import com.etsviewer.core.service.*
import kotlinx.serialization.json.JsonObject

class QuestionAnswerParser : QuestionTypeParser {
    override val supportedTypes = setOf("collector.role", "collector.3q5a", "collector.dialogue")
    override fun parse(info: JsonObject, context: ParseContext): ParsedSectionContent {
        val questions = info.array("question").mapIndexed { index, raw ->
            val q = raw as? JsonObject ?: throw ProtocolFailure("Invalid question at $index")
            QuestionDefinition(index, text = TextNormalizer.clean(q.string("ask")), answers = ParseFields.variants(q),
                media = MediaExtractor.collect(q), explanation = ParseFields.explanation(q))
        }
        return ParseFields.content(info, questions)
    }
}

