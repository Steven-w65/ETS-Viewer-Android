package com.etsviewer.android.screens

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.etsviewer.core.accounts.LearnerAccount
import com.etsviewer.android.ui.*

@Composable fun AccountSelectionScreen(accounts: List<LearnerAccount>, onSelect: (String) -> Unit, onCancel: () -> Unit) {
    val texts = rememberUiStrings()
    LazyColumn(Modifier.widthIn(max = AppTokens.formWidth).fillMaxSize(), contentPadding = PaddingValues(AppTokens.large),
        verticalArrangement = Arrangement.spacedBy(AppTokens.medium)) {
        item { AppHeading(texts.text(R.string.account_select), texts.text(R.string.account_select_body)) }
        itemsIndexed(accounts, key = { _, account -> account.key }) { index, account -> AppAccountCard(account, index + 1) { onSelect(account.key) } }
        if (accounts.isEmpty()) item { AppEmptyState(texts.text(R.string.account_empty), texts.text(R.string.account_empty_body)) }
        item { AppSecondaryButton(texts.text(R.string.back_login), onCancel, Modifier.fillMaxWidth()) }
    }
}
