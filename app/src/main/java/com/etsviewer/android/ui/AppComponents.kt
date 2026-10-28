package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.etsviewer.core.accounts.LearnerAccount
import com.etsviewer.core.homework.*
import kotlin.math.cos
import kotlin.math.sin

@Composable fun AppScaffold(snackbar: SnackbarHostState, content: @Composable () -> Unit) {
    val noticeHeader = remember { mutableStateOf(0.dp) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        CompositionLocalProvider(LocalNoticeHeaderHeight provides noticeHeader) {
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                content()
                FloatingNoticeHost(snackbar, Modifier.align(Alignment.TopEnd).padding(top = noticeHeader.value))
            }
        }
    }
}
fun themeLabel(theme: String, texts: UiStrings) = when (theme) { "dark" -> texts.text(R.string.theme_dark); "light" -> texts.text(R.string.theme_light); else -> texts.text(R.string.theme_system) }
fun switchColorMode(isDark: Boolean, readTheme: () -> String, cycleTheme: () -> Unit) {
    val target = if (isDark) "light" else "dark"
    repeat(3) { if (readTheme() != target) cycleTheme() }
}
@Composable fun AppThemeToggle(theme: String, onTheme: () -> Unit) {
    val texts = rememberUiStrings()
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    IconButton(onTheme, Modifier.sizeIn(minWidth = AppTokens.touch, minHeight = AppTokens.touch).testTag("theme-toggle")
        .semantics { contentDescription = texts.text(R.string.theme_toggle_description, if (dark) texts.text(R.string.theme_light) else texts.text(R.string.theme_dark), themeLabel(theme, texts)); stateDescription = themeLabel(theme, texts) }) {
        val color = LocalContentColor.current
        Canvas(Modifier.size(AppTokens.icon)) {
            scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                if (dark) {
                    drawCircle(color, 4f, Offset(12f, 12f))
                    repeat(8) { ray ->
                        val angle = ray * Math.PI / 4
                        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                        drawLine(color, Offset(12f, 12f) + direction * 7f, Offset(12f, 12f) + direction * 10f, 1.8f, StrokeCap.Round)
                    }
                } else drawPath(Path().apply {
                    moveTo(20f, 15.5f)
                    cubicTo(10f, 18f, 6f, 10f, 10f, 3f)
                    cubicTo(-2f, 6f, 1f, 23f, 14f, 21f)
                    cubicTo(17f, 20.5f, 19f, 18f, 20f, 15.5f)
                    close()
                }, color)
            }
        }
    }
}
@Composable fun AppPage(modifier: Modifier = Modifier, form: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = if (form) AppTokens.formWidth else AppTokens.pageWidth).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(AppTokens.large),
            verticalArrangement = Arrangement.spacedBy(AppTokens.medium), content = content)
    }
}
@Composable fun AppHeading(title: String, description: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun AppPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick, modifier.heightIn(min = AppTokens.control), enabled, shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = AppTokens.medium, vertical = AppTokens.compact)) {
        icon?.let { Icon(it, null, Modifier.size(AppTokens.statusIcon)); Spacer(Modifier.width(AppTokens.small)) }
        Text(text)
    }
}
@Composable fun AppCompactAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick, modifier.heightIn(min = AppTokens.touch), enabled = enabled,
        shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = AppTokens.small, vertical = AppTokens.small)) { Text(text) }
}
@Composable fun AppSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    OutlinedButton(onClick, modifier.heightIn(min = AppTokens.control), enabled, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(AppTokens.border, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = AppTokens.medium, vertical = AppTokens.compact)) {
        icon?.let { Icon(it, null, Modifier.size(AppTokens.statusIcon)); Spacer(Modifier.width(AppTokens.small)) }; Text(text)
    }
}
@Composable fun AppTextField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, error: String? = null, hint: String? = null, keyboard: KeyboardOptions = KeyboardOptions.Default,
    transformation: VisualTransformation = VisualTransformation.None, leadingIcon: ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    OutlinedTextField(value, onValue, modifier.fillMaxWidth().heightIn(min = AppTokens.control), enabled = enabled,
        label = { Text(label) }, singleLine = true, isError = error != null, shape = MaterialTheme.shapes.medium,
        supportingText = if (error != null || hint != null) ({ Text(error ?: hint.orEmpty()) }) else null,
        keyboardOptions = keyboard, visualTransformation = transformation, trailingIcon = trailing,
        leadingIcon = leadingIcon?.let { { Icon(it, null, Modifier.size(AppTokens.icon)) } })
}
enum class StatusTone { NEUTRAL, PROGRESS, SUCCESS, WARNING, ERROR }
@Composable fun AppStatusChip(label: String, tone: StatusTone = StatusTone.NEUTRAL, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme; val status = LocalStatusColors.current
    val (background, foreground) = when (tone) {
        StatusTone.SUCCESS -> status.success to status.onSuccess
        StatusTone.WARNING -> status.warning to status.onWarning
        StatusTone.ERROR -> colors.errorContainer to colors.onErrorContainer
        StatusTone.PROGRESS -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.surfaceContainerHigh to colors.onSurfaceVariant
    }
    val icon = when (tone) { StatusTone.SUCCESS -> Icons.Outlined.CheckCircle; StatusTone.ERROR, StatusTone.WARNING -> Icons.Outlined.Warning; StatusTone.PROGRESS -> Icons.Outlined.Refresh; else -> Icons.Outlined.Info }
    Surface(color = background, contentColor = foreground, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(horizontal = if (compact) AppTokens.small else AppTokens.compact, vertical = if (compact) AppTokens.micro else AppTokens.small).semantics(mergeDescendants = true) { stateDescription = label },
            horizontalArrangement = Arrangement.spacedBy(AppTokens.small), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(AppTokens.statusIcon)); Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
@Composable fun DownloadStatusChip(phase: HomeworkReadPhase, compact: Boolean = false) {
    val texts = rememberUiStrings()
    when (phase) {
    HomeworkReadPhase.NOT_DOWNLOADED -> AppStatusChip(texts.text(R.string.status_not_downloaded), compact = compact)
    HomeworkReadPhase.DOWNLOADING -> AppStatusChip(texts.text(R.string.status_downloading), StatusTone.PROGRESS, compact)
    HomeworkReadPhase.PROCESSING -> AppStatusChip(texts.text(R.string.status_processing), StatusTone.PROGRESS, compact)
    HomeworkReadPhase.READY -> AppStatusChip(texts.text(R.string.status_downloaded), StatusTone.SUCCESS, compact)
    HomeworkReadPhase.FAILED -> AppStatusChip(texts.text(R.string.status_download_failed), StatusTone.ERROR, compact)
    }
}
@Composable fun AppLoadingIndicator(label: String, fraction: Float? = null) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(AppTokens.medium).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(AppTokens.compact)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.compact)) {
                if (fraction == null) CircularProgressIndicator(Modifier.size(AppTokens.progress), strokeWidth = AppTokens.micro)
                else Icon(Icons.Outlined.Refresh, null, Modifier.size(AppTokens.icon), tint = MaterialTheme.colorScheme.primary)
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (fraction != null) Text("${(fraction.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            if (fraction != null) LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
@Composable fun AppEmptyState(title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = AppTokens.xlarge), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.compact)) {
        Icon(Icons.Outlined.Info, null, Modifier.size(AppTokens.progress), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) AppSecondaryButton(action, onAction)
    }
}
@Composable fun AppAccountCard(account: LearnerAccount, number: Int? = null, onClick: () -> Unit) {
    val texts = rememberUiStrings()
    OutlinedCard(onClick, Modifier.fillMaxWidth().testTag("account-${account.key}"), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(AppTokens.border, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(AppTokens.compact).heightIn(min = AppTokens.touch), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.medium)) {
            Icon(Icons.Outlined.Person, null, Modifier.size(AppTokens.icon), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTokens.micro)) {
                if (number != null) Text(texts.text(R.string.account_number, number), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(account.name.ifBlank { texts.text(R.string.account_fallback) }, style = MaterialTheme.typography.titleMedium)
                Text(listOf(account.grade, account.className).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { texts.text(R.string.account_open_hint) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
        }
    }
}
@Composable fun HomeworkCard(entry: HomeworkEntry, record: HomeworkReadState, enabled: Boolean, onClick: () -> Unit) {
    val texts = rememberUiStrings()
    OutlinedCard(onClick, Modifier.fillMaxWidth().testTag("homework-card-${entry.id}"), enabled, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(AppTokens.border, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(AppTokens.compact), verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
                Text(entry.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(AppTokens.statusIcon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
                Text(texts.text(R.string.home_resource_count, entry.resources.size), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DownloadStatusChip(if (record.isReadable) HomeworkReadPhase.READY else record.phase, compact = true)
            }
            if (record.isPartial) Text(texts.text(R.string.partial_unavailable), style = MaterialTheme.typography.bodySmall, color = LocalStatusColors.current.onWarning)
            if (!record.isReadable && record.phase == HomeworkReadPhase.FAILED) Text(texts.text(R.string.retry_download_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
