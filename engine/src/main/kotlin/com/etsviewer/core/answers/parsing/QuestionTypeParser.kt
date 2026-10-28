package com.etsviewer.core.answers.parsing

import com.etsviewer.core.answers.*
import kotlinx.serialization.json.JsonObject

interface QuestionTypeParser {
    val supportedTypes: Set<String>
    fun parse(info: JsonObject, context: ParseContext): ParsedSectionContent
}

data class ParseContext(val structureType: String)

data class ParsedSectionContent(
    val questions: List<QuestionDefinition>,
    val media: List<MediaReference> = emptyList(),
    val originalText: String = "",
    val explanation: String = "",
    val allowsSectionMediaOnly: Boolean = false,
)

data class QuestionParserRegistration(
    val structureType: String,
    val displayName: String,
    val parser: QuestionTypeParser,
    val presentation: QuestionTypePresentation = QuestionTypePresentation(),
)

