package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.etsviewer.core.answers.*

@Composable fun SectionContentGroup(section: AnswerSection, sectionIndex: Int, multiple: Boolean, identity: String, media: @Composable (MediaReference) -> Unit) =
    SectionContentGroup(section, sectionIndex, multiple, identity, "", media)

@Composable fun SectionContentGroup(section: AnswerSection, sectionIndex: Int, multiple: Boolean, identity: String,
    resourceTitle: String, media: @Composable (MediaReference) -> Unit) {
    val texts = rememberUiStrings()
    val policy = ReaderPresentationPolicy.forSection(section)
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
        val heading = sectionDisplayHeading(section, resourceTitle, multiple, questionTypeName(section, texts), texts.text(R.string.section_part, sectionIndex + 1))
        if (heading.isNotBlank()) Text(heading, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        ExpandableMaterial(texts.text(R.string.material_source), section.originalText, "$identity/source")
        ExpandableMaterial(texts.text(R.string.material_explanation), section.explanation, "$identity/explanation")
        policy.visibleMedia(section.media).forEach { media(it) }
    }
}
@Composable fun QuestionContentCard(section: AnswerSection, question: QuestionDefinition, identity: String,
    answerTag: String, explanationTag: String, media: @Composable (MediaReference) -> Unit) {
    val texts = rememberUiStrings()
    val presentation = ReaderPresentationPolicy.forSection(section).present(question)
    val prompt = androidx.compose.runtime.remember(question.number, question.text) { questionDisplayPrompt(question) }
    ReaderContentSurface(Modifier.testTag("question-card-$identity")) {
        Column(if (presentation.hasBody) Modifier.testTag("question-body-$identity") else Modifier,
            verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.compact), verticalAlignment = Alignment.Top) {
                QuestionNumberBadge(question.number)
                if (prompt.isNotBlank() || !question.topic.isNullOrBlank()) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTokens.micro)) {
                    if (!question.topic.isNullOrBlank()) SelectionContainer { Text(texts.text(R.string.question_topic, question.topic.orEmpty()), style = MaterialTheme.typography.titleMedium) }
                    if (prompt.isNotBlank()) SelectionContainer { Text(prompt, style = MaterialTheme.typography.bodyLarge) }
                }
            }
            if (presentation.originalMaterialDisplay == OriginalMaterialDisplay.INLINE) {
                if (question.originalText.isNotBlank()) SelectionContainer { Text(question.originalText, style = MaterialTheme.typography.bodyLarge) }
            } else ExpandableMaterial(texts.text(R.string.material_source), question.originalText, "$identity/source")
            QuestionOptions(presentation.options, media)
            presentation.media.forEach { media(it) }
        }
        ReferenceAnswerCard(question.answers, identity, answerTag)
        ExpandableMaterial(texts.text(R.string.material_translation), question.translation, "$identity/translation")
        ExpandableMaterial(texts.text(R.string.material_explanation), question.explanation, "$identity/explanation", explanationTag)
    }
}

@Composable private fun QuestionNumberBadge(number: String) {
    val texts = rememberUiStrings()
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small) {
        Text(texts.text(R.string.question_number, number), Modifier.padding(horizontal = AppTokens.small, vertical = AppTokens.micro), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable private fun QuestionOptions(options: List<AnswerOption>, media: @Composable (MediaReference) -> Unit) {
    val texts = rememberUiStrings()
    options.forEach { option ->
        SelectionContainer {
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { text = AnnotatedString(texts.text(R.string.option_text, option.label, option.text)) },
                horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
                Text(texts.text(R.string.option_label, option.label), Modifier.widthIn(min = AppTokens.icon), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(option.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
        option.media.forEach { media(it) }
    }
}
