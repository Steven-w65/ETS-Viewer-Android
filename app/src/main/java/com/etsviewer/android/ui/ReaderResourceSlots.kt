package com.etsviewer.android.ui

import com.etsviewer.core.answers.*
import com.etsviewer.core.homework.ResourceGroup
import com.etsviewer.core.resources.RemoteResourceLoader
import com.etsviewer.android.R

enum class ResourceNameSource { CLOUD_GROUP, PARSED_PAGE, FAILURE_RECORD, POSITION_FALLBACK }
enum class ResourceSlotStatus { READ, PARTIAL, SHARED, FAILED, READING }

/** Cloud positions are navigation slots; only real extracted pages carry Sections. */
data class ReaderResourceSlot(
    val originalGroupIndex: Int, val groupName: String, val page: ResourceReaderPage?,
    val failures: List<ResourceReadFailure>, val sharedWithIndex: Int? = null,
    val nameSource: ResourceNameSource = ResourceNameSource.PARSED_PAGE,
) {
    val status: ResourceSlotStatus get() = when {
        page != null && failures.isNotEmpty() -> ResourceSlotStatus.PARTIAL
        page != null -> ResourceSlotStatus.READ
        sharedWithIndex != null -> ResourceSlotStatus.SHARED
        else -> ResourceSlotStatus.FAILED
    }
    fun displayStatus(isRetrying: Boolean): ResourceSlotStatus =
        if (isRetrying && (failures.isNotEmpty() || (page == null && sharedWithIndex == null))) ResourceSlotStatus.READING else status
    fun displayName(texts: UiStrings): String = groupName.ifBlank { texts.text(R.string.resource_number, originalGroupIndex + 1) }
}

fun resourceStatusName(status: ResourceSlotStatus, texts: UiStrings): String = texts.text(when (status) {
    ResourceSlotStatus.READ -> R.string.status_read
    ResourceSlotStatus.PARTIAL -> R.string.status_partial
    ResourceSlotStatus.SHARED -> R.string.status_shared
    ResourceSlotStatus.FAILED -> R.string.status_read_failed
    ResourceSlotStatus.READING -> R.string.status_reading
})

fun readerResourceSlots(document: AnswerDocument, groups: List<ResourceGroup> = emptyList(), baseUrl: String = ""): List<ReaderResourceSlot> {
    val pages = document.resourcePages.associateBy { it.originalGroupIndex }
    val failures = document.resourceFailures.groupBy { it.originalGroupIndex }
    val count = maxOf(document.totalResourceCount, groups.size, (pages.keys + failures.keys).maxOrNull()?.plus(1) ?: 0)
    val urls = groups.map { group -> runCatching { RemoteResourceLoader.resolve(baseUrl, group.url) }.getOrNull() }
    return (0 until count).map { index ->
        val page = pages[index]
        val failed = failures[index].orEmpty()
        val shared = if (page == null && failed.isEmpty() && urls.getOrNull(index) != null)
            pages.keys.firstOrNull { urls.getOrNull(it) == urls[index] } else null
        val names = listOf(ResourceNameSource.CLOUD_GROUP to groups.getOrNull(index)?.name,
            ResourceNameSource.PARSED_PAGE to page?.groupName, ResourceNameSource.FAILURE_RECORD to failed.firstOrNull()?.groupName)
        val name = names.firstOrNull { !it.second.isNullOrBlank() }
        ReaderResourceSlot(index, name?.second.orEmpty(), page, failed, shared, name?.first ?: ResourceNameSource.POSITION_FALLBACK)
    }
}
