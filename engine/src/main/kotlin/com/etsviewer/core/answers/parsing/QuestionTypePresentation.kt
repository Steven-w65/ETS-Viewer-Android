package com.etsviewer.core.answers.parsing

/** Platform-neutral keys: Android supplies localized labels, never a Context to the parser. */
enum class QuestionTypeLabel { QUESTION_ANSWER, THREE_QUESTIONS, CHOICE, RETELL, FILL, DIALOGUE, PARAGRAPH_READING, SENTENCE_READING }
enum class MaterialDisplay { COLLAPSIBLE, INLINE }
data class QuestionTypePresentation(
    val label: QuestionTypeLabel? = null,
    val audioEnabled: Boolean = true,
    val originalMaterialDisplay: MaterialDisplay = MaterialDisplay.COLLAPSIBLE,
)
