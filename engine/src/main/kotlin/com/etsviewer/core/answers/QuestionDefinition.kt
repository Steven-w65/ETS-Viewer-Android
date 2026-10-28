package com.etsviewer.core.answers

data class AnswerOption(val label: String, val text: String, val media: List<MediaReference> = emptyList())
enum class MediaKind { IMAGE, AUDIO, VIDEO }
data class MediaReference(val kind: MediaKind, val reference: String)
data class QuestionDefinition(
    val index: Int, val number: String = (index + 1).toString(), val text: String = "",
    val answers: List<String> = emptyList(), val options: List<AnswerOption> = emptyList(),
    val topic: String? = null, val originalText: String = "", val media: List<MediaReference> = emptyList(),
    val explanation: String = "",
    val translation: String = "",
)
data class AnswerSection(
    val name: String, val structureType: String, val typeName: String,
    val questions: List<QuestionDefinition>, val media: List<MediaReference> = emptyList(), val mediaRoot: String = "",
    val mediaBoundary: String = "",
    val originalText: String = "", val explanation: String = "",
    val sourceContentPath: String = "",
    val sectionTitle: SectionTitle? = null,
    val nameOrigin: SectionNameOrigin = SectionNameOrigin.PROVIDED,
    val presentation: com.etsviewer.core.answers.parsing.QuestionTypePresentation? = null,
)
data class ResourceReaderPage(
    val resourceId: String, val groupName: String, val resourceFolderPath: String,
    val originalGroupIndex: Int, val sections: List<AnswerSection>,
) {
    init { require(resourceId.isNotBlank() && resourceFolderPath.isNotBlank() && sections.any { it.questions.isNotEmpty() }) }
}
data class ResourceReadFailure(val resourceId: String, val groupName: String, val originalGroupIndex: Int, val message: String)

data class AnswerDocument(
    val title: String, val sections: List<AnswerSection>, val failures: List<String> = emptyList(),
    val resourcePages: List<ResourceReaderPage> = emptyList(), val resourceFailures: List<ResourceReadFailure> = emptyList(),
    val totalResourceCount: Int = 0, val readingId: String = "",
) {
    val isReadable: Boolean get() = resourcePages.any { page -> page.sections.any { it.questions.isNotEmpty() } }
    val isComplete: Boolean get() = isReadable && failures.isEmpty() && resourceFailures.isEmpty()
    companion object {
        /** Keeps the existing export view without deriving reader boundaries from Section names/counts. */
        fun fromResources(title: String, pages: List<ResourceReaderPage>, failures: List<ResourceReadFailure> = emptyList(),
            totalResourceCount: Int = (pages.map { it.originalGroupIndex } + failures.map { it.originalGroupIndex }).maxOrNull()?.plus(1) ?: 0,
            readingId: String = "", additionalFailures: List<String> = emptyList()): AnswerDocument {
            val ordered = pages.distinctBy { it.resourceId }.sortedBy { it.originalGroupIndex }
            // The legacy summary is also exported. Keep raw diagnostics only in structured metadata.
            return AnswerDocument(title, ordered.flatMap { it.sections }, failures.map { "${it.groupName}: 资源暂时无法读取，请重试。" } + additionalFailures,
                ordered, failures.sortedBy { it.originalGroupIndex }, totalResourceCount, readingId)
        }
    }
}
