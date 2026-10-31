package com.etsviewer.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val LocalNoticeHeaderHeight = compositionLocalOf<MutableState<Dp>?> { null }

/** Measured inside the already inset-safe app content area. */
@Composable fun Modifier.noticeHeaderAnchor(extraTop: Dp = 0.dp): Modifier {
    val anchor = LocalNoticeHeaderHeight.current
    val density = LocalDensity.current
    DisposableEffect(anchor) { onDispose { anchor?.value = 0.dp } }
    return onSizeChanged { size -> anchor?.value = with(density) { size.height.toDp() } + extraTop }
}

private data class BriefNoticeVisuals(override val message: String, val occurrenceId: Long?) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction = true
    override val duration = SnackbarDuration.Indefinite
}

suspend fun SnackbarHostState.showBriefNotice(message: String, occurrenceId: Long? = null) {
    if (message.isBlank()) return
    currentSnackbarData?.let { current ->
        if (occurrenceId != null && (current.visuals as? BriefNoticeVisuals)?.occurrenceId == occurrenceId) return
        current.dismiss()
    }
    showSnackbar(BriefNoticeVisuals(message, occurrenceId))
}

@Composable fun FloatingNoticeHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val data = state.currentSnackbarData
    var last by remember { mutableStateOf<SnackbarData?>(null) }
    SideEffect { if (data != null) last = data }
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(data, accessibility) {
        data?.let {
            val timeout = accessibility?.calculateRecommendedTimeoutMillis(2000L, containsIcons = true, containsText = true, containsControls = true) ?: 2000L
            delay(timeout)
            it.dismiss()
        }
    }
    AnimatedVisibility(data != null, modifier.padding(horizontal = AppTokens.small, vertical = AppTokens.small).widthIn(max = 320.dp).fillMaxWidth(),
        enter = slideInHorizontally(tween(180)) { it } + fadeIn(tween(180)),
        exit = slideOutHorizontally(tween(150)) { it } + fadeOut(tween(150))) {
        (data ?: last)?.let { AppSnackbar(it) }
    }
}
