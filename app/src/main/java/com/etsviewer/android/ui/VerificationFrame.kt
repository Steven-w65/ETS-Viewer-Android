package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The service web page keeps its own challenge implementation; only the native surrounding UI is themed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun VerificationFrame(loading: Boolean, error: String, message: String, expired: Boolean,
    onCancel: () -> Unit, onRetry: () -> Unit, content: @Composable (Modifier) -> Unit) {
    val texts = rememberUiStrings()
    BoxWithConstraints(Modifier.widthIn(max = AppTokens.pageWidth).fillMaxSize()) {
        val browserHeight = maxOf(AppTokens.browserMin, maxHeight * .7f)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(AppTokens.large), verticalArrangement = Arrangement.spacedBy(AppTokens.medium)) {
            AppHeading(texts.text(R.string.verification_title), texts.text(R.string.verification_body))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
                AppSecondaryButton(texts.text(R.string.verification_cancel), onCancel)
                AppSecondaryButton(texts.text(R.string.verification_retry), onRetry)
            }
            if (expired) AppStatusChip(texts.text(R.string.verification_expired), StatusTone.WARNING)
            if (loading) AppLoadingIndicator(texts.text(R.string.verification_loading))
            if (error.isNotBlank() || message.isNotBlank()) AppErrorState(error + "\n" + message, ErrorContext.VERIFICATION, onRetry)
            content(Modifier.fillMaxWidth().height(browserHeight))
        }
    }
}
