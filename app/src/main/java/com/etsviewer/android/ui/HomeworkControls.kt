package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.etsviewer.core.homework.HomeworkPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeworkCategoryTabs(period: HomeworkPeriod, enabled: Boolean, onPeriod: (HomeworkPeriod) -> Unit, onRefresh: () -> Unit) {
    val texts = rememberUiStrings()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTokens.small), verticalAlignment = Alignment.CenterVertically) {
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f).heightIn(min = AppTokens.touch)) {
            HomeworkPeriod.entries.forEachIndexed { index, value ->
                SegmentedButton(selected = period == value, onClick = { onPeriod(value) }, enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, HomeworkPeriod.entries.size, AppPillShape),
                    modifier = Modifier.heightIn(min = AppTokens.touch).testTag(if (value == HomeworkPeriod.CURRENT) "current" else "history"),
                    label = { Text(if (value == HomeworkPeriod.CURRENT) texts.text(R.string.home_current) else texts.text(R.string.home_history), style = MaterialTheme.typography.labelLarge) })
            }
        }
        IconButton(onRefresh, Modifier.size(AppTokens.touch).testTag("refresh"), enabled = enabled) {
            Icon(Icons.Outlined.Refresh, texts.text(R.string.home_refresh), Modifier.size(AppTokens.icon), tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun HomeworkSearchField(query: String, onQuery: (String) -> Unit) {
    val texts = rememberUiStrings()
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().heightIn(min = AppTokens.toolbar).testTag("search")
        .semantics { contentDescription = texts.text(R.string.home_search) },
        placeholder = { Text(texts.text(R.string.home_search)) }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
        shape = AppPillShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent),
        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(AppTokens.icon)) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton({ onQuery("") }, Modifier.size(AppTokens.touch).testTag("clear-search")) { Icon(Icons.Outlined.Clear, texts.text(R.string.search_clear)) }
        }) else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }))
}
