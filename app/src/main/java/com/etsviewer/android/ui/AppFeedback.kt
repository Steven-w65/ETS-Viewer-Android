package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import com.etsviewer.core.identity.AccessUiState

@Composable fun AppSnackbar(data: SnackbarData) {
    val texts = rememberUiStrings()
    Surface(Modifier.testTag("floating-notice").semantics { liveRegion = LiveRegionMode.Polite }, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, shadowElevation = AppTokens.small) {
        Row(Modifier.fillMaxWidth().padding(start = AppTokens.compact, end = AppTokens.small, top = AppTokens.small, bottom = AppTokens.small),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            Text(data.visuals.message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            data.visuals.actionLabel?.let { TextButton(data::performAction, Modifier.heightIn(min = AppTokens.touch)) { Text(it) } }
            IconButton(data::dismiss, Modifier.sizeIn(minWidth = AppTokens.touch, minHeight = AppTokens.touch).testTag("close-notice")) { Icon(Icons.Outlined.Close, texts.text(R.string.notice_close)) }
        }
    }
}
@Composable fun AppNoticeEffect(message: String, snackbar: SnackbarHostState) {
    val texts = rememberUiStrings()
    LaunchedEffect(message) { FeedbackPresenter(texts).notice(message)?.let { snackbar.showBriefNotice(it) } }
}
@Composable fun AppErrorMessage(error: UiError, onRetry: (() -> Unit)? = null, retryLabel: String = androidx.compose.ui.res.stringResource(R.string.action_retry), retryTag: String = "retry") =
    CompactOperationError(error, onRetry, retryLabel, retryTag)
@Composable fun AppErrorState(raw: String, context: ErrorContext, onRetry: (() -> Unit)? = null, retryTag: String = "retry", retryLabel: String = androidx.compose.ui.res.stringResource(R.string.action_retry)) =
    AppErrorMessage(FeedbackPresenter(rememberUiStrings()).error(raw, context), onRetry, retryLabel, retryTag)
@Composable fun AppConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    cancel: String? = androidx.compose.ui.res.stringResource(R.string.action_cancel), warning: Boolean = false) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("app-dialog"), shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(if (warning) Icons.Outlined.Warning else Icons.Outlined.Info, null, tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(message, style = MaterialTheme.typography.bodyMedium) } },
        confirmButton = { TextButton(onConfirm, Modifier.heightIn(min = AppTokens.touch).testTag("dialog-confirm")) { Text(confirm) } },
        dismissButton = if (cancel == null) null else ({ TextButton(onDismiss, Modifier.heightIn(min = AppTokens.touch).testTag("dialog-cancel")) { Text(cancel) } }))
}
@Composable fun AppSessionFeedback(identity: AccessUiState) {
    val texts = rememberUiStrings()
    var wasSignedIn by rememberSaveable { mutableStateOf(false) }
    var show by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(identity) {
        when (identity) {
            is AccessUiState.SignedIn -> { wasSignedIn = true; show = false }
            is AccessUiState.Failure -> if (wasSignedIn) { wasSignedIn = false; show = true }
            is AccessUiState.SignedOut -> { wasSignedIn = false; show = false }
            else -> Unit
        }
    }
    if (show) AppConfirmDialog(texts.text(R.string.feedback_session_title), texts.text(R.string.feedback_session_body), texts.text(R.string.login_relogin), { show = false }, { show = false }, cancel = null)
}
