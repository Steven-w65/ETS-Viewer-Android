package com.etsviewer.android.ui

import com.etsviewer.core.answers.*
import com.etsviewer.core.answers.parsing.*

typealias OriginalMaterialDisplay = MaterialDisplay

/** Remove only a repeated source number; never alter parsed or exported content. */
fun questionDisplayPrompt(question: QuestionDefinition): String {
    val number = question.number.trim()
    if (number.isEmpty()) return question.text
    val n = Regex.escape(number)
    val prefix = Regex("^\\s*(?:$n[.．:：]\\s+|$n[、)）]\\s*|[（(]$n[)）]\\s*)")
    val match = prefix.find(question.text) ?: return question.text
    return question.text.substring(match.range.last + 1).takeIf { it.isNotBlank() } ?: question.text
}

fun sectionDisplayHeading(section: AnswerSection, resourceTitle: String, multiple: Boolean,
    typeLabel: String = section.typeName, partLabel: String = ""): String {
    val ownTitle = section.sectionTitle ?: sectionTitleFromDirectory(section.sourceContentPath.replace('\\', '/').substringBeforeLast('/', ""))
    val legacyOwnName = section.name.takeIf { section.nameOrigin == SectionNameOrigin.PROVIDED }?.trim()
        ?.takeUnless { it == resourceTitle.trim() || it == section.typeName.trim() }.orEmpty()
    val name = (ownTitle?.value?.takeIf(String::isNotBlank) ?: legacyOwnName)
        .takeUnless { !multiple && it.trim() == resourceTitle.trim() }.orEmpty()
    return listOf(name.ifBlank { if (multiple) partLabel else "" }, typeLabel.trim()).filter(String::isNotBlank).distinct().joinToString(" · ")
}

/** Presentation-only decisions. Parsed answer data remains available to export and other consumers. */
data class ReaderPresentationPolicy(
    val originalMaterialDisplay: OriginalMaterialDisplay = OriginalMaterialDisplay.COLLAPSIBLE,
    val audioEnabled: Boolean = true,
) {
    fun visibleMedia(references: List<MediaReference>): List<MediaReference> =
        if (audioEnabled) references else references.filterNot { it.kind == MediaKind.AUDIO }

    fun present(question: QuestionDefinition) = ReaderQuestionPresentation(question,
        if (audioEnabled) question.options else question.options.map { it.copy(media = visibleMedia(it.media)) },
        visibleMedia(question.media), originalMaterialDisplay)

    companion object {
        private val registry = QuestionParserRegistry()
        fun forSection(section: AnswerSection): ReaderPresentationPolicy =
            (section.presentation ?: registry.presentationFor(section.structureType)).let { ReaderPresentationPolicy(it.originalMaterialDisplay, it.audioEnabled) }
        fun forStructure(type: String, registrations: QuestionParserRegistry = registry): ReaderPresentationPolicy =
            registrations.presentationFor(type).let { ReaderPresentationPolicy(it.originalMaterialDisplay, it.audioEnabled) }
    }
}

data class ReaderQuestionPresentation(
    val question: QuestionDefinition,
    val options: List<AnswerOption>,
    val media: List<MediaReference>,
    val originalMaterialDisplay: OriginalMaterialDisplay,
) {
    val hasBody: Boolean get() = question.text.isNotBlank() || !question.topic.isNullOrBlank() ||
        question.originalText.isNotBlank() || options.isNotEmpty() || media.isNotEmpty()
}

