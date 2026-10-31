package com.etsviewer.android.screens

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.etsviewer.android.ui.*

@Composable
fun DeviceBindingScreen(onBind: () -> Unit, onCancel: () -> Unit) {
    val texts = rememberUiStrings()
    AppPage(form = true) {
        AppStatusChip(texts.text(R.string.binding_required), StatusTone.WARNING)
        AppHeading(texts.text(R.string.binding_title), texts.text(R.string.binding_body))
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
            Text(texts.text(R.string.binding_service_body), Modifier.padding(AppTokens.medium), style = MaterialTheme.typography.bodyMedium)
        }
        AppPrimaryButton(texts.text(R.string.binding_action), onBind, Modifier.fillMaxWidth().testTag("bind-device"))
        AppSecondaryButton(texts.text(R.string.back_login), onCancel, Modifier.fillMaxWidth().testTag("cancel-binding"))
        Text(texts.text(R.string.binding_retry_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
