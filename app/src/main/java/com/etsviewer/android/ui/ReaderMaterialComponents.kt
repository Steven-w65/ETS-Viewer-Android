package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*

@Composable fun ExpandableMaterial(title: String, text: String, identity: String, tag: String = identity, tint: Color = MaterialTheme.colorScheme.primary) {
    val texts = rememberUiStrings()
    if (text.isBlank()) return
    var expanded by rememberSaveable(identity) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
        Row(Modifier.fillMaxWidth().heightIn(min = AppTokens.touch).clickable(role = Role.Button,
            onClickLabel = if (expanded) texts.text(R.string.action_collapse, title) else texts.text(R.string.action_expand, title), onClick = { expanded = !expanded }).testTag(tag)
            .semantics(mergeDescendants = true) { stateDescription = if (expanded) texts.text(R.string.expanded) else texts.text(R.string.collapsed) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = tint)
            Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(AppTokens.statusIcon), tint = tint)
        }
        if (expanded) SelectionContainer { Text(text, style = MaterialTheme.typography.bodyLarge) }
    }
}
@Composable internal fun ReaderContentSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppTokens.border, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(AppTokens.medium), verticalArrangement = Arrangement.spacedBy(AppTokens.compact), content = content)
    }
}
