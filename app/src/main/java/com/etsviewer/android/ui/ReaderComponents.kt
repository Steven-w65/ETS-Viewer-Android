package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow

@Composable fun ResourceBackButton(onBack: () -> Unit) {
    val texts = rememberUiStrings()
    IconButton(onBack, Modifier.sizeIn(minWidth = AppTokens.touch, minHeight = AppTokens.touch).testTag("back-to-homework")) {
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, texts.text(R.string.reader_back), Modifier.size(AppTokens.icon))
    }
}
@Composable fun ResourcePageHeader(slot: ReaderResourceSlot, count: Int, status: ResourceSlotStatus, onBack: () -> Unit, actions: @Composable () -> Unit) {
    val texts = rememberUiStrings()
    BoxWithConstraints {
    if (maxWidth >= 600.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            ResourceBackButton(onBack)
            Text(slot.displayName(texts), Modifier.weight(1f).testTag("resource-heading").semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(texts.text(R.string.resource_position, slot.originalGroupIndex + 1, count), Modifier.testTag("resource-position"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ResourceStatusChip(status)
            actions()
        }
    } else {
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.micro)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            ResourceBackButton(onBack)
            Text(slot.displayName(texts), Modifier.weight(1f).testTag("resource-heading").semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            actions()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            Text(texts.text(R.string.resource_position, slot.originalGroupIndex + 1, count), Modifier.weight(1f).testTag("resource-position"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ResourceStatusChip(status)
        }
    }
    }
    }
}
@Composable private fun ResourceStatusChip(status: ResourceSlotStatus) {
    val texts = rememberUiStrings()
    AppStatusChip(resourceStatusName(status, texts), when (status) {
        ResourceSlotStatus.READ, ResourceSlotStatus.SHARED -> StatusTone.SUCCESS
        ResourceSlotStatus.READING -> StatusTone.PROGRESS
        else -> StatusTone.WARNING
    }, compact = true)
}
@Composable fun ResourcePageNavigation(index: Int, slots: List<ReaderResourceSlot>, isRetrying: Boolean, onSelect: (Int) -> Unit) {
    val texts = rememberUiStrings()
    var menu by remember { mutableStateOf(false) }
    // This opaque surface stays mounted while only the scrollable slot content changes.
    Surface(Modifier.fillMaxWidth().testTag("resource-navigation"), color = MaterialTheme.colorScheme.surface.copy(alpha = 1f)) {
        BoxWithConstraints {
        val stacked = maxWidth < 360.dp && LocalDensity.current.fontScale > 1.2f
        Row(Modifier.fillMaxWidth().padding(horizontal = AppTokens.medium, vertical = AppTokens.micro),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            ResourceNavigationButton(texts.text(R.string.resource_previous), { onSelect(index - 1) }, Modifier.weight(1f).testTag("previous-resource"),
                index > 0 && slots.isNotEmpty(), forward = false, stacked = stacked)
            Box {
                AppCompactAction(texts.text(R.string.page_counter, if (slots.isEmpty()) 0 else index + 1, slots.size), { menu = true },
                    Modifier.widthIn(min = AppTokens.touch).testTag("resource-selector").semantics {
                        contentDescription = texts.text(R.string.resource_selector_description, if (slots.isEmpty()) 0 else index + 1, slots.size)
                    }, enabled = slots.isNotEmpty())
                DropdownMenu(menu, { menu = false }, Modifier.heightIn(max = 360.dp).testTag("resource-menu"), shape = MaterialTheme.shapes.medium) {
                    slots.forEach { slot ->
                        DropdownMenuItem(text = {
                            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.micro)) {
                                Text(texts.text(R.string.numbered_name, slot.originalGroupIndex + 1, slot.displayName(texts)))
                                Text(resourceStatusName(slot.displayStatus(isRetrying), texts), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }, onClick = { menu = false; onSelect(slot.originalGroupIndex) },
                            modifier = Modifier.heightIn(min = AppTokens.touch).testTag("select-resource-${slot.originalGroupIndex}")
                                .semantics { selected = slot.originalGroupIndex == index },
                            trailingIcon = if (slot.originalGroupIndex == index) ({ Text(texts.text(R.string.resource_current), style = MaterialTheme.typography.labelSmall) }) else null)
                    }
                }
            }
            ResourceNavigationButton(texts.text(R.string.resource_next), { onSelect(index + 1) }, Modifier.weight(1f).testTag("next-resource"),
                index >= 0 && index < slots.lastIndex, forward = true, stacked = stacked)
        }
        }
    }
}

@Composable private fun ResourceNavigationButton(label: String, onClick: () -> Unit, modifier: Modifier,
    enabled: Boolean, forward: Boolean, stacked: Boolean) {
    val content: @Composable RowScope.() -> Unit = {
        val arrow = if (forward) Icons.AutoMirrored.Outlined.KeyboardArrowRight else Icons.AutoMirrored.Outlined.KeyboardArrowLeft
        if (stacked) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(arrow, null, Modifier.size(AppTokens.statusIcon))
            Text(label, style = MaterialTheme.typography.labelMedium)
        } else {
            if (!forward) { Icon(arrow, null, Modifier.size(AppTokens.statusIcon)); Spacer(Modifier.width(AppTokens.micro)) }
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (forward) { Spacer(Modifier.width(AppTokens.micro)); Icon(arrow, null, Modifier.size(AppTokens.statusIcon)) }
        }
    }
    val padding = PaddingValues(horizontal = AppTokens.small, vertical = AppTokens.micro)
    if (forward) Button(onClick, modifier.heightIn(min = AppTokens.touch), enabled = enabled,
        shape = AppPillShape, contentPadding = padding, content = content)
    else OutlinedButton(onClick, modifier.heightIn(min = AppTokens.touch), enabled = enabled,
        shape = AppPillShape, border = BorderStroke(AppTokens.border, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = padding, content = content)
}
