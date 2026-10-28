package com.etsviewer.android

import com.etsviewer.android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etsviewer.core.answers.AnswerExport
import com.etsviewer.core.identity.AccessUiState
import com.etsviewer.android.screens.*
import com.etsviewer.android.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: MainViewModel = viewModel()
            val identity by model.identity.collectAsStateWithLifecycle()
            val state by model.homework.collectAsStateWithLifecycle()
            val readingStates by model.readingStates.collectAsStateWithLifecycle()
            val theme by model.theme.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            fun notice(value: String) { scope.launch { snackbar.showBriefNotice(value) } }
            var messageContext by remember { mutableStateOf(ErrorContext.HOMEWORK) }
            var dismissedReaderMessage by remember { mutableStateOf("") }
            LaunchedEffect(state.message) { if (state.message.isBlank()) dismissedReaderMessage = "" }
            val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
                if (uri == null) notice(getString(R.string.notice_export_cancelled))
                model.export(uri)
            }
            val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
                }
            }
            AppTheme(dark) {
                AppNoticeEffect(state.message, snackbar)
                AppNoticeEffect((identity as? AccessUiState.SignedOut)?.message.orEmpty(), snackbar)
                AppScaffold(snackbar) {
                    when (val access = identity) {
                        is AccessUiState.VerificationRequired -> {
                            BackHandler { model.cancelChallenge() }
                            key(access.requestId) {
                                ChallengeScreen({ model.challenge(it, access.requestId) }, model::cancelChallenge,
                                    { model.rejectChallenge(it, access.requestId) }, model::retryChallenge, access.message, access.expired)
                            }
                        }
                        is AccessUiState.BindingRequired -> {
                            BackHandler { model.cancelBinding() }
                            DeviceBindingScreen(access.message, model::bindDevice, model::cancelBinding)
                        }
                        is AccessUiState.ChooseAccount -> AccountSelectionScreen(access.accounts, model::selectAccount, model::logout)
                        is AccessUiState.SignedIn -> {
                            val document = state.reader
                            if (document != null) {
                                HomeworkReaderScreen(document, { dismissedReaderMessage = state.message; model.closeReader() }, onExport = {
                                    messageContext = ErrorContext.EXPORT; model.exportDocument = it; export.launch(AnswerExport.filename(it.title))
                                }, message = state.message, onRetry = { messageContext = ErrorContext.DOWNLOAD; model.retrySelectedHomework() },
                                    isRetrying = state.readerRetrying, progress = state.progress, messageContext = messageContext,
                                    resourceGroups = state.selectedHomework?.resources.orEmpty(), baseUrl = state.selectedBaseUrl)
                            } else HomeworkListScreen(state.batch, state.period, state.query, state.loading, state.message.takeUnless { it == dismissedReaderMessage }.orEmpty(), state.progress,
                                model::query, { messageContext = ErrorContext.HOMEWORK; model.refresh(it) },
                                { messageContext = ErrorContext.HOMEWORK; model.refresh() }, { messageContext = ErrorContext.DOWNLOAD; model.open(it) },
                                { model.logout(); notice(getString(R.string.notice_logged_out)) }, { messageContext = ErrorContext.CACHE; model.clearCache() },
                                readingStates, state.clearingCache, messageContext, ::notice, theme,
                                { switchColorMode(dark, { model.theme.value }, model::cycleTheme) })
                        }
                        else -> PhoneAccessScreen(
                            initialPhone = when (access) {
                                is AccessUiState.Failure -> access.phone
                                is AccessUiState.SignedOut -> access.phone.ifBlank { model.enteredPhone }
                                else -> model.enteredPhone
                            }, initialPassword = model.enteredPassword, busy = access is AccessUiState.Working,
                            message = when (access) {
                                is AccessUiState.Failure -> access.message
                                is AccessUiState.Working -> access.message
                                is AccessUiState.SignedOut -> access.message
                                else -> ""
                            }, onLogin = model::login,
                            onRestore = if (access is AccessUiState.Failure) model::retrySavedSession else null)
                    }
                    AppSessionFeedback(identity)
                }
            }
        }
    }
}
