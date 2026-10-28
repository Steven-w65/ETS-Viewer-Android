package com.etsviewer.android.ui

import com.etsviewer.android.R
import com.etsviewer.core.answers.AnswerSection
import com.etsviewer.core.answers.parsing.QuestionParserRegistry
import com.etsviewer.core.answers.parsing.QuestionTypeLabel

private val typeRegistry = QuestionParserRegistry()

/** This adapter translates label keys; all type-id/capability registration stays in the core. */
fun questionTypeName(section: AnswerSection, texts: UiStrings): String = when ((section.presentation ?: typeRegistry.presentationFor(section.structureType)).label) {
    QuestionTypeLabel.QUESTION_ANSWER -> texts.text(R.string.type_question_answer)
    QuestionTypeLabel.THREE_QUESTIONS -> texts.text(R.string.type_three_questions)
    QuestionTypeLabel.CHOICE -> texts.text(R.string.type_choice)
    QuestionTypeLabel.RETELL -> texts.text(R.string.type_retell)
    QuestionTypeLabel.FILL -> texts.text(R.string.type_fill)
    QuestionTypeLabel.DIALOGUE -> texts.text(R.string.type_dialogue)
    QuestionTypeLabel.PARAGRAPH_READING -> texts.text(R.string.type_paragraph_reading)
    QuestionTypeLabel.SENTENCE_READING -> texts.text(R.string.type_sentence_reading)
    null -> section.typeName.ifBlank { texts.text(R.string.type_unknown) }
}
