package com.etsviewer.android

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
import com.etsviewer.core.identity.AccessWork
import com.etsviewer.core.feedback.*
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
            fun notice(value: NoticeKind) {
                val occurrence = AppFeedback.Notice(value)
                scope.launch { snackbar.showBriefNotice(FeedbackPresenter(AndroidUiStrings(resources)).notice(value), occurrence.id) }
            }
            var dismissedReaderFeedbackId by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(state.feedback) { if (state.feedback == null) dismissedReaderFeedbackId = null }
            val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
                if (uri == null) notice(NoticeKind.EXPORT_CANCELLED)
                model.export(uri)
            }
            val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
                }
            }
            AppTheme(dark) {
                AppNoticeEffect(state.feedback, snackbar)
                AppNoticeEffect((identity as? AccessUiState.SignedOut)?.notice, snackbar)
                AppScaffold(snackbar) {
                    when (val access = identity) {
                        is AccessUiState.VerificationRequired -> {
                            BackHandler { model.cancelChallenge() }
                            key(access.requestId) {
                                ChallengeScreen({ model.challenge(it, access.requestId) }, model::cancelChallenge,
                                    { model.rejectChallenge(it, access.requestId) }, model::retryChallenge, access.failure, access.expired)
                            }
                        }
                        is AccessUiState.BindingRequired -> {
                            BackHandler { model.cancelBinding() }
                            DeviceBindingScreen(model::bindDevice, model::cancelBinding)
                        }
                        is AccessUiState.ChooseAccount -> AccountSelectionScreen(access.accounts, model::selectAccount, model::logout)
                        is AccessUiState.SignedIn -> {
                            val document = state.reader
                            if (document != null) {
                                HomeworkReaderScreen(document, { dismissedReaderFeedbackId = state.feedback?.id; model.closeReader() }, onExport = {
                                    model.exportDocument = it; export.launch(AnswerExport.filename(it.title))
                                }, feedback = state.feedback, onRetry = model::retrySelectedHomework,
                                    isRetrying = state.readerRetrying, progress = state.progress,
                                    resourceGroups = state.selectedHomework?.resources.orEmpty(), baseUrl = state.selectedBaseUrl)
                            } else HomeworkListScreen(state.batch, state.period, state.query, state.loading, state.feedback.takeUnless { it?.id == dismissedReaderFeedbackId }, state.progress,
                                model::query, { model.refresh(it) }, { model.refresh() }, model::open,
                                { model.logout(); notice(NoticeKind.LOGGED_OUT) }, model::clearCache,
                                readingStates, state.clearingCache, ::notice, theme,
                                { switchColorMode(dark, { model.theme.value }, model::cycleTheme) })
                        }
                        else -> PhoneAccessScreen(
                            initialPhone = when (access) {
                                is AccessUiState.Failure -> access.phone
                                is AccessUiState.SignedOut -> access.phone.ifBlank { model.enteredPhone }
                                else -> model.enteredPhone
                            }, initialPassword = model.enteredPassword, busy = access is AccessUiState.Working,
                            failure = (access as? AccessUiState.Failure)?.failure,
                            workingStage = (access as? AccessUiState.Working)?.stage ?: AccessWork.LOGIN, onLogin = model::login,
                            onRestore = if (access is AccessUiState.Failure) model::retrySavedSession else null)
                    }
                    AppSessionFeedback(identity)
                }
            }
        }
    }
}
