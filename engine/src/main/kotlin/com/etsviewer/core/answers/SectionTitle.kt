package com.etsviewer.core.answers

enum class SectionTitleSource { CONTENT_METADATA, CONTENT_DIRECTORY }
enum class SectionNameOrigin { PROVIDED, RESOURCE_COMPATIBILITY }
data class SectionTitle(val value: String, val source: SectionTitleSource)

/** The path comes from the verified extraction root; a root content.json has no Section folder. */
fun sectionTitleFromDirectory(relativeParent: String): SectionTitle? = relativeParent.replace('\\', '/')
    .split('/').lastOrNull { it.isNotBlank() && it != "." && it != ".." }
    ?.let { SectionTitle(it, SectionTitleSource.CONTENT_DIRECTORY) }
