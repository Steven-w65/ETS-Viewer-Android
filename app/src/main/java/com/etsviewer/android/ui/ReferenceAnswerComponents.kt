package com.etsviewer.android.ui

import com.etsviewer.android.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable fun ReferenceAnswerCard(answers: List<String>, identity: String, tag: String) {
    val valid = answers.filter { it.isNotBlank() }
    if (valid.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.compact)) {
        MainReferenceAnswerCard(valid.first(), identity)
        if (valid.size > 1) AdditionalReferenceAnswersCard(valid.drop(1), identity, tag)
    }
}

@Composable private fun MainReferenceAnswerCard(answer: String, identity: String) {
    val texts = rememberUiStrings()
    val colors = LocalStatusColors.current
    Surface(Modifier.fillMaxWidth().testTag("main-answer-$identity"), color = colors.answer, contentColor = colors.onAnswer,
        shape = MaterialTheme.shapes.medium, border = BorderStroke(AppTokens.border, colors.answerAccent.copy(alpha = .3f))) {
        val compact = answer.trim().matches(Regex("[A-Za-z]"))
        Column(Modifier.padding(AppTokens.compact), verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTokens.small)) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(AppTokens.statusIcon), tint = colors.answerAccent)
                Text(texts.text(R.string.answer_reference), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = colors.answerAccent)
                if (compact) SelectionContainer { Text(answer, style = MaterialTheme.typography.titleMedium) }
            }
            if (!compact) SelectionContainer { Text(answer, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable private fun AdditionalReferenceAnswersCard(answers: List<String>, identity: String, tag: String) {
    val texts = rememberUiStrings()
    ReaderContentSurface(Modifier.testTag("additional-answers-$identity")) {
        ExpandableMaterial(texts.text(R.string.answer_additional_count, answers.size), answers.joinToString("\n\n"), "$identity/alternatives", tag)
    }
}
