package com.etsviewer.android.screens

import com.etsviewer.android.R
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.etsviewer.core.answers.*
import com.etsviewer.core.homework.ResourceGroup
import com.etsviewer.core.resources.ResourceProgress
import com.etsviewer.android.ui.*
import kotlinx.coroutines.flow.collect

@Composable
fun HomeworkReaderScreen(document: AnswerDocument, onBack: () -> Unit, onExport: (AnswerDocument) -> Unit,
    message: String = "", onRetry: (() -> Unit)? = null, isRetrying: Boolean = false,
    progress: ResourceProgress? = null, messageContext: ErrorContext = ErrorContext.EXPORT,
    resourceGroups: List<ResourceGroup> = emptyList(), baseUrl: String = "") {
    val texts = rememberUiStrings()
    PredictiveBackHandler { events -> events.collect {}; onBack() }
    val readingId = document.readingId.ifBlank { document.title }
    val slots = remember(document, resourceGroups, baseUrl) { readerResourceSlots(document, resourceGroups, baseUrl) }
    var selectedIndex by rememberSaveable(readingId) { mutableIntStateOf(document.resourcePages.firstOrNull()?.originalGroupIndex ?: 0) }
    var warningExpanded by rememberSaveable(readingId) { mutableStateOf(false) }
    val index = selectedIndex.coerceIn(0, slots.lastIndex.coerceAtLeast(0))
    val slot = slots.getOrNull(index)
    val stateHolder = rememberSaveableStateHolder()
    Scaffold(Modifier.fillMaxSize().testTag("reader-screen"), containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Surface(Modifier.fillMaxWidth().noticeHeaderAnchor().testTag("reader-header"), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(horizontal = AppTokens.medium, vertical = AppTokens.micro)) {
                    if (slot != null) ResourcePageHeader(slot, slots.size, slot.displayStatus(isRetrying), onBack) { ReaderOverflow(document, onExport) }
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        ResourceBackButton(onBack)
                        Text(texts.text(R.string.reader_no_resource), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        ReaderOverflow(document, onExport)
                    }
                }
            }
        },
        bottomBar = { ResourcePageNavigation(index, slots, isRetrying) { if (it in slots.indices) selectedIndex = it } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            val identity = slot?.page?.resourceId ?: "slot-$index"
            stateHolder.SaveableStateProvider("$readingId/$identity") {
                ResourceSlotContent(document, slot, onExport, warningExpanded, { warningExpanded = !warningExpanded },
                    onRetry, isRetrying, progress, message, messageContext, { selectedIndex = it })
            }
        }
    }
}

@Composable private fun ReaderOverflow(document: AnswerDocument, onExport: (AnswerDocument) -> Unit) {
    val texts = rememberUiStrings()
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton({ menu = true }, Modifier.sizeIn(minWidth = AppTokens.touch, minHeight = AppTokens.touch).testTag("reader-more")) {
            Icon(Icons.Outlined.MoreVert, texts.text(R.string.reader_actions))
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text(texts.text(R.string.answer_export)) }, onClick = { menu = false; onExport(document) }, enabled = document.isReadable,
                modifier = Modifier.heightIn(min = AppTokens.touch).testTag("export"))
        }
    }
}

@Composable private fun ResourceSlotContent(document: AnswerDocument, slot: ReaderResourceSlot?,
    onExport: (AnswerDocument) -> Unit, warningExpanded: Boolean, onWarningToggle: () -> Unit,
    onRetry: (() -> Unit)?, isRetrying: Boolean, progress: ResourceProgress?, message: String, messageContext: ErrorContext,
    onSelect: (Int) -> Unit) {
    val texts = rememberUiStrings()
    val list = rememberLazyListState()
    val page = slot?.page
    LazyColumn(Modifier.widthIn(max = AppTokens.pageWidth).fillMaxSize().testTag("reader-content"), state = list,
        contentPadding = PaddingValues(horizontal = AppTokens.medium, vertical = AppTokens.small),
        verticalArrangement = Arrangement.spacedBy(AppTokens.compact)) {
        if (document.failures.isNotEmpty() || document.resourceFailures.isNotEmpty()) item("resource-warning") {
            CompactResourceWarning(document, warningExpanded, onWarningToggle, onRetry, isRetrying)
        }
        if (isRetrying) item("retry-progress") {
            Text(if (document.isReadable) texts.text(R.string.retry_readable_body) else texts.text(R.string.retry_unreadable_body), style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { progress?.fraction?.coerceIn(0f, 1f) ?: 0f }, modifier = Modifier.fillMaxWidth())
        }
        if (message.isNotBlank() && FeedbackPresenter(texts).notice(message) == null) item("message") {
            AppErrorState(message, messageContext, if (messageContext == ErrorContext.EXPORT) ({ onExport(document) }) else onRetry)
        }
        if (page == null) item("unavailable") {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(AppTokens.compact), verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
                    val shared = slot?.sharedWithIndex
                    Text(if (shared != null) texts.text(R.string.resource_shared_body, shared + 1) else if (slot == null) texts.text(R.string.reader_no_readable) else texts.text(R.string.resource_unavailable_body),
                        style = MaterialTheme.typography.bodyMedium)
                    if (shared != null) AppCompactAction(texts.text(R.string.resource_open_shared), { onSelect(shared) }, Modifier.testTag("open-shared-resource"))
                    else onRetry?.let {
                        AppCompactAction(if (isRetrying) texts.text(R.string.retrying) else texts.text(R.string.retry_download), it,
                            Modifier.testTag(if (slot == null) "retry-resources" else "retry-unavailable-resource"), !isRetrying)
                    }
                }
            }
        }
        page?.sections?.forEachIndexed { sectionIndex, section ->
            val sectionId = "${page.resourceId}/${section.sourceContentPath.ifBlank { "section-$sectionIndex" }}"
            item("$sectionId/header") {
                SectionContentGroup(section, sectionIndex, page.sections.size > 1, sectionId, slot?.groupName.orEmpty()) { MediaContent(it, section.mediaRoot, section.mediaBoundary) }
            }
            section.questions.forEachIndexed { questionIndex, question ->
                val identity = "$sectionId/question-$questionIndex-${question.number}-${question.text.hashCode()}-${question.originalText.hashCode()}"
                item(identity) {
                    QuestionContentCard(section, question, identity, "expand-answers-${page.resourceId}-$sectionIndex-$questionIndex",
                        "expand-explanation-${page.resourceId}-$sectionIndex-$questionIndex") { MediaContent(it, section.mediaRoot, section.mediaBoundary) }
                }
            }
        }
    }
}
