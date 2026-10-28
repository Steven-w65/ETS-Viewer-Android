package com.etsviewer.android.screens

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import com.etsviewer.core.homework.*
import com.etsviewer.core.resources.ResourceProgress
import com.etsviewer.android.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeworkListScreen(batch: HomeworkBatch, period: HomeworkPeriod, query: String, loading: Boolean = false,
    message: String = "", progress: ResourceProgress? = null, onQuery: (String) -> Unit, onPeriod: (HomeworkPeriod) -> Unit,
    onRefresh: () -> Unit, onOpen: (HomeworkEntry) -> Unit, onLogout: () -> Unit, onClearCache: () -> Unit,
    readingStates: Map<String, HomeworkReadState> = emptyMap(), clearingCache: Boolean = false,
    messageContext: ErrorContext = ErrorContext.HOMEWORK, onNotice: (String) -> Unit = {},
    theme: String = "system", onTheme: () -> Unit = {}) {
    val texts = rememberUiStrings()
    val busy = loading || progress != null || clearingCache
    var confirmation by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val visible = remember(batch, query) { batch.search(query) }
    val itemKeys = remember(batch) {
        val occurrences = mutableMapOf<String, Int>()
        batch.entries.map { entry ->
            val identity = entry.readingKey(batch.baseUrl)
            val occurrence = occurrences.getOrDefault(identity, 0)
            occurrences[identity] = occurrence + 1
            entry to "$identity/$occurrence"
        }
    }
    val visibleKeys = remember(itemKeys, query) { itemKeys.filter { it.first.name.contains(query.trim(), ignoreCase = true) }.map { it.second } }
    val hasError = message.isNotBlank() && FeedbackPresenter(texts).notice(message) == null
    LazyColumn(Modifier.widthIn(max = AppTokens.pageWidth).fillMaxSize(), contentPadding = PaddingValues(AppTokens.medium),
        verticalArrangement = Arrangement.spacedBy(AppTokens.compact)) {
        item {
            Column(Modifier.noticeHeaderAnchor(AppTokens.medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(texts.text(R.string.home_brand), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    AppThemeToggle(theme, onTheme)
                    Box {
                        IconButton({ menu = true }, Modifier.sizeIn(minWidth = AppTokens.touch, minHeight = AppTokens.touch).testTag("homework-more")) { Icon(Icons.Outlined.MoreVert, texts.text(R.string.home_more)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(texts.text(R.string.cache_clear)) }, onClick = { menu = false; confirmation = "cache" }, enabled = !busy, modifier = Modifier.heightIn(min = AppTokens.touch).testTag("clear-cache"))
                            DropdownMenuItem(text = { Text(texts.text(R.string.logout)) }, onClick = { menu = false; confirmation = "logout" }, modifier = Modifier.heightIn(min = AppTokens.touch).testTag("logout"))
                        }
                    }
                }
                Spacer(Modifier.height(AppTokens.small))
                HomeworkCategoryTabs(period, !busy, onPeriod, onRefresh)
                Spacer(Modifier.height(AppTokens.compact))
                HomeworkSearchField(query, onQuery)
            }
        }
        if (loading) item { AppLoadingIndicator(texts.text(R.string.home_loading)) }
        progress?.let { item { AppLoadingIndicator(if (it.phase == HomeworkReadPhase.PROCESSING) texts.text(R.string.resource_processing) else texts.text(R.string.resource_downloading), it.fraction) } }
        if (clearingCache) item { AppLoadingIndicator(texts.text(R.string.cache_clearing)) }
        if (hasError && (messageContext != ErrorContext.DOWNLOAD || readingStates.values.none { it.phase == HomeworkReadPhase.FAILED }))
            item { AppErrorState(message, messageContext, if (messageContext == ErrorContext.CACHE) ({ confirmation = "cache" }) else onRefresh, "retry-homework-list") }
        if (!busy && !hasError && visible.isEmpty()) item {
            AppEmptyState(if (query.isBlank()) texts.text(R.string.home_empty) else texts.text(R.string.search_empty), if (query.isBlank()) texts.text(R.string.home_empty_body) else texts.text(R.string.search_empty_body),
                if (query.isBlank()) texts.text(R.string.action_refresh) else texts.text(R.string.search_clear), if (query.isBlank()) onRefresh else ({ onQuery("") }))
        }
        if (visible.isNotEmpty()) item { Text(texts.text(R.string.home_result_count, visible.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(visible, key = { index, _ -> visibleKeys[index] }) { _, entry ->
            val record = readingStates[entry.readingKey(batch.baseUrl)] ?: HomeworkReadState()
            HomeworkCard(entry, record, !busy, { onOpen(entry) })
        }
    }
    when (confirmation) {
        "cache" -> AppConfirmDialog(texts.text(R.string.cache_confirm_title), texts.text(R.string.cache_confirm_body), texts.text(R.string.cache_clear), { confirmation = ""; onClearCache() }, { confirmation = ""; onNotice(texts.text(R.string.notice_cancelled)) }, warning = true)
        "logout" -> AppConfirmDialog(texts.text(R.string.logout_confirm_title), texts.text(R.string.logout_confirm_body), texts.text(R.string.logout), { confirmation = ""; onLogout() }, { confirmation = ""; onNotice(texts.text(R.string.notice_cancelled)) }, warning = true)
    }
}
