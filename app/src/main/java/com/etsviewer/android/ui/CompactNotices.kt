package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import com.etsviewer.core.answers.AnswerDocument

@Composable fun ExpandableNotice(title: String, expanded: Boolean, onToggle: () -> Unit, tag: String,
    background: Color, foreground: Color, icon: ImageVector = Icons.Outlined.Info, iconTint: Color = foreground,
    content: @Composable ColumnScope.() -> Unit) {
    val texts = rememberUiStrings()
    Surface(color = background, contentColor = foreground, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = AppTokens.touch)
                .clickable(role = Role.Button, onClickLabel = if (expanded) texts.text(R.string.action_collapse, title) else texts.text(R.string.action_expand, title), onClick = onToggle)
                .testTag(tag).semantics(mergeDescendants = true) { stateDescription = if (expanded) texts.text(R.string.expanded) else texts.text(R.string.collapsed) }
                .padding(horizontal = AppTokens.compact, vertical = AppTokens.small),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.small), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(AppTokens.statusIcon), tint = iconTint)
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(AppTokens.statusIcon))
            }
            if (expanded) Column(Modifier.padding(start = AppTokens.compact, end = AppTokens.compact, bottom = AppTokens.compact),
                verticalArrangement = Arrangement.spacedBy(AppTokens.small), content = content)
        }
    }
}

@Composable fun ExpandableErrorDetails(raw: String, context: ErrorContext, identity: String) {
    val texts = rememberUiStrings()
    var expanded by rememberSaveable(identity) { mutableStateOf(false) }
    AppCompactAction(if (expanded) texts.text(R.string.details_collapse) else texts.text(R.string.details_expand), { expanded = !expanded })
    if (expanded) SelectionContainer { Text(FeedbackPresenter(texts).error(raw, context).details, style = MaterialTheme.typography.bodySmall) }
}

@Composable fun CompactOperationError(error: UiError, onRetry: (() -> Unit)?, retryLabel: String, retryTag: String) {
    val texts = rememberUiStrings()
    var expanded by rememberSaveable(error.title, error.message) { mutableStateOf(false) }
    var details by rememberSaveable(error.title, error.message) { mutableStateOf(false) }
    ExpandableNotice(error.title, expanded, { expanded = !expanded }, "operation-error", MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.onSurface, Icons.Outlined.Warning, MaterialTheme.colorScheme.error) {
        Text(error.message, style = MaterialTheme.typography.bodyMedium)
        Text(error.explanation, style = MaterialTheme.typography.bodySmall)
        onRetry?.let { AppCompactAction(retryLabel, it, Modifier.testTag(retryTag)) }
        AppCompactAction(if (details) texts.text(R.string.details_collapse) else texts.text(R.string.details_expand), { details = !details })
        if (details) SelectionContainer { Text(error.details, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable fun CompactResourceWarning(document: AnswerDocument, expanded: Boolean, onToggle: () -> Unit,
    onRetry: (() -> Unit)?, isRetrying: Boolean) {
    val texts = rememberUiStrings()
    val semantic = LocalStatusColors.current
    val partial = document.isReadable
    val raw = document.resourceFailures.joinToString("\n") { it.message }.ifBlank { document.failures.joinToString("\n") }
    ExpandableNotice(if (partial) texts.text(R.string.resource_partial_warning) else texts.text(R.string.resource_failed_warning), expanded, onToggle, "resource-warning",
        if (partial) semantic.warning else MaterialTheme.colorScheme.surfaceContainerHigh,
        if (partial) semantic.onWarning else MaterialTheme.colorScheme.onSurface, Icons.Outlined.Warning) {
        val failed = document.resourceFailures.distinctBy { it.originalGroupIndex }
        val count = if (failed.isNotEmpty()) failed.size else minOf(document.totalResourceCount, document.failures.size)
        Text(if (count > 0) texts.text(R.string.resource_failure_count, count) else texts.text(R.string.resource_none_downloadable), style = MaterialTheme.typography.bodyMedium)
        failed.forEach { failure -> Text(texts.text(R.string.numbered_name, failure.originalGroupIndex + 1, failure.groupName.ifBlank { texts.text(R.string.resource_unnamed) }), style = MaterialTheme.typography.bodySmall) }
        Text(FeedbackPresenter(texts).error(raw, ErrorContext.DOWNLOAD).message, style = MaterialTheme.typography.bodySmall)
        onRetry?.let { AppCompactAction(if (isRetrying) texts.text(R.string.retrying) else texts.text(R.string.retry_failed_resources), it,
            Modifier.testTag("retry-resources"), enabled = !isRetrying) }
        ExpandableErrorDetails(raw, ErrorContext.DOWNLOAD, "resource-details-${document.readingId}")
    }
}

@Composable fun CompactMediaError(title: String, raw: String, identity: String, onRetry: (() -> Unit)? = null) {
    val texts = rememberUiStrings()
    var expanded by rememberSaveable(identity) { mutableStateOf(false) }
    val colors = LocalStatusColors.current
    ExpandableNotice(title, expanded, { expanded = !expanded }, "media-notice-$identity", colors.media, colors.onMedia) {
        Text(FeedbackPresenter(texts).error(raw, ErrorContext.MEDIA).message, style = MaterialTheme.typography.bodySmall)
        onRetry?.let { AppCompactAction(texts.text(R.string.action_retry), it, Modifier.testTag("retry-media-$identity")) }
        ExpandableErrorDetails(raw, ErrorContext.MEDIA, "media-details-$identity")
    }
}
