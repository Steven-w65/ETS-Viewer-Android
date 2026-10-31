package com.etsviewer.android.screens

import com.etsviewer.android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.*
import com.etsviewer.android.ui.*
import com.etsviewer.core.feedback.AppFailure
import com.etsviewer.core.identity.AccessWork

@Composable
fun PhoneAccessScreen(initialPhone: String = "", initialPassword: String = "", busy: Boolean = false, failure: AppFailure? = null,
    workingStage: AccessWork = AccessWork.LOGIN,
    onLogin: (String, String, Boolean) -> Unit, onRestore: (() -> Unit)? = null) {
    val texts = rememberUiStrings()
    var phone by rememberSaveable(initialPhone) { mutableStateOf(initialPhone) }
    // Password stays in memory, never in saved-instance-state.
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }
    var show by remember { mutableStateOf(false) }
    var rememberPassword by rememberSaveable { mutableStateOf(false) }
    var validation by remember { mutableStateOf(LoginValidation()) }
    var editedSinceError by remember(failure, busy) { mutableStateOf(false) }
    val error = if (!busy && !editedSinceError) failure?.let { FeedbackPresenter(texts).error(it) } else null
    AppPage(form = true) {
        AppStatusChip(texts.text(R.string.login_cloud))
        AppHeading(texts.text(R.string.login_welcome), texts.text(R.string.login_welcome_body))
        AppTextField(phone, { phone = it; validation = validation.copy(phone = null); editedSinceError = true }, texts.text(R.string.login_phone), Modifier.testTag("phone"), enabled = !busy,
            error = validation.phone ?: error?.takeIf { it.field == FormField.PHONE }?.message,
            hint = texts.text(R.string.login_phone_hint), keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), leadingIcon = Icons.Outlined.Phone)
        AppTextField(password, { password = it; validation = validation.copy(password = null); editedSinceError = true }, texts.text(R.string.login_password), Modifier.testTag("password"), enabled = !busy,
            error = validation.password ?: error?.takeIf { it.field == FormField.PASSWORD }?.message,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), leadingIcon = Icons.Outlined.Lock,
            transformation = if (show) VisualTransformation.None else PasswordVisualTransformation(), trailing = {
                TextButton({ show = !show }, Modifier.heightIn(min = AppTokens.touch).testTag("show-password"), enabled = !busy) { Text(if (show) texts.text(R.string.password_hide) else texts.text(R.string.password_show)) }
            })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(rememberPassword, { rememberPassword = it }, Modifier.sizeIn(minWidth = AppTokens.touch, minHeight = AppTokens.touch).testTag("remember-password"), enabled = !busy)
            Text(texts.text(R.string.password_remember), style = MaterialTheme.typography.bodyMedium)
        }
        Text(texts.text(R.string.password_remember_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppPrimaryButton(if (busy) texts.text(R.string.login_busy) else texts.text(R.string.login_action), {
            validation = FeedbackPresenter(texts).validateLogin(phone, password)
            if (validation.isValid) onLogin(phone, password, rememberPassword)
        }, Modifier.fillMaxWidth().testTag("login"), enabled = !busy)
        if (busy) AppLoadingIndicator(FeedbackPresenter(texts).working(workingStage))
        if (error != null && error.field == null) AppErrorMessage(error)
        onRestore?.let { AppSecondaryButton(texts.text(R.string.login_restore_retry), it, Modifier.fillMaxWidth()) }
    }
}
