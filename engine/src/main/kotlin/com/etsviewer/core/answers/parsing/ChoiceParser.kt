package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.*
import com.etsviewer.core.service.*
import kotlinx.serialization.json.JsonObject

class ChoiceParser : QuestionTypeParser {
    override val supportedTypes = setOf("collector.choose")
    override fun parse(info: JsonObject, context: ParseContext): ParsedSectionContent {
        val questions = info.array("xtlist").mapIndexed { index, raw ->
            val q = raw as? JsonObject ?: throw ProtocolFailure("Invalid choice at $index")
            QuestionDefinition(index, q.string("xt_xh").ifBlank { (index + 1).toString() }, TextNormalizer.clean(q.string("xt_nr")),
                listOf(q.string("answer")), q.array("xxlist").map { option ->
                    val o = option as? JsonObject ?: throw ProtocolFailure("Invalid choice option")
                    AnswerOption(o.string("xx_mc"), TextNormalizer.clean(o.string("xx_nr")), MediaExtractor.collect(o))
                }, media = MediaExtractor.collect(JsonObject(q.filterKeys { it != "xxlist" })), explanation = ParseFields.explanation(q))
        }
        return ParseFields.content(info, questions)
    }
}

