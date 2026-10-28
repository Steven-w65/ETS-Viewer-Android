package com.etsviewer.core.answers.parsing

import com.etsviewer.core.service.ProtocolFailure

/** A stable exact-id lookup. Aliases can share one parser while retaining their own names. */
class QuestionParserRegistry(registrations: List<QuestionParserRegistration> = defaults()) {
    private val entries = linkedMapOf<String, QuestionParserRegistration>()
    val typeNames: Map<String, String>

    init {
        for (entry in registrations) {
            require(entry.structureType.isNotBlank() && entry.displayName.isNotBlank()) { "Question type and name must not be blank" }
            require(entry.structureType in entry.parser.supportedTypes) { "Parser does not support ${entry.structureType}" }
            require(entry.structureType !in entries) { "Duplicate question structure: ${entry.structureType}" }
            entries[entry.structureType] = entry
        }
        typeNames = entries.mapValues { it.value.displayName }
    }

    internal fun resolve(type: String): QuestionParserRegistration =
        entries[type] ?: throw ProtocolFailure("Unsupported question structure: $type")

    fun presentationFor(type: String): QuestionTypePresentation = entries[type]?.presentation ?: QuestionTypePresentation()

    companion object {
        private fun defaults(): List<QuestionParserRegistration> {
            val questions = QuestionAnswerParser()
            val reading = ReadingParser()
            return listOf(
                // Compatibility names preserve the existing core/export contract; native UI uses label keys.
                QuestionParserRegistration("collector.role", "问答题", questions, QuestionTypePresentation(QuestionTypeLabel.QUESTION_ANSWER)),
                QuestionParserRegistration("collector.3q5a", "3问5答", questions, QuestionTypePresentation(QuestionTypeLabel.THREE_QUESTIONS)),
                QuestionParserRegistration("collector.choose", "选择题", ChoiceParser(), QuestionTypePresentation(QuestionTypeLabel.CHOICE, audioEnabled = false)),
                QuestionParserRegistration("collector.picture", "信息转述", RetellParser(), QuestionTypePresentation(QuestionTypeLabel.RETELL)),
                QuestionParserRegistration("collector.fill", "填空题", FillParser(), QuestionTypePresentation(QuestionTypeLabel.FILL)),
                QuestionParserRegistration("collector.dialogue", "对话问答", questions, QuestionTypePresentation(QuestionTypeLabel.DIALOGUE)),
                QuestionParserRegistration("collector.read", "模仿朗读", reading, QuestionTypePresentation(QuestionTypeLabel.PARAGRAPH_READING, originalMaterialDisplay = MaterialDisplay.INLINE)),
                QuestionParserRegistration("collector.word", "词句朗读", reading, QuestionTypePresentation(QuestionTypeLabel.SENTENCE_READING, originalMaterialDisplay = MaterialDisplay.INLINE)),
            )
        }
    }
}

