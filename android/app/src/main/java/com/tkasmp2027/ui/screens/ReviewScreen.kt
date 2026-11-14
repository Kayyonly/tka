package com.tkasmp2027.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkasmp2027.model.AnswerKey
import com.tkasmp2027.model.QuestionGradeStatus
import com.tkasmp2027.model.QuestionItem
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuizEvaluation
import com.tkasmp2027.model.UserAnswer
import com.tkasmp2027.ui.components.RichHtmlContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    pkg: QuestionPackage,
    answers: Map<String, UserAnswer>,
    evaluation: QuizEvaluation,
    onBackToResult: () -> Unit,
    onViewActivityLog: () -> Unit
) {
    val resultById = evaluation.questionResults.associateBy { it.questionId }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBackToResult) { Text("Hasil") }
                },
                title = {
                    Text("Tinjauan — ${pkg.subjectName} ${pkg.title}")
                },
                actions = {
                    TextButton(onClick = onViewActivityLog) {
                        Text("Riwayat")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pkg.questions, key = { it.id }) { question ->
                val eval = resultById[question.id]
                val statusText = when (eval?.status) {
                    QuestionGradeStatus.CORRECT -> "Benar"
                    QuestionGradeStatus.INCORRECT -> "Salah"
                    QuestionGradeStatus.UNANSWERED -> "Belum Dijawab"
                    else -> "Menunggu Kunci Resmi"
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nomor ${question.number} · ${question.id} · ${question.type.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        RichHtmlContent(html = question.questionHtml)

                        Text(
                            text = "Jawaban Anda: ${formatUserAnswer(question, answers[question.id])}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Kunci Jawaban: ${formatAnswerKey(question)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!question.explanationHtml.isNullOrBlank()) {
                            RichHtmlContent(
                                html = question.explanationHtml,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun choiceLabel(question: QuestionItem, order: Int?): String {
    if (order == null) return "—"
    return question.statementChoices.firstOrNull { it.order == order }?.label ?: order.toString()
}

private fun formatUserAnswer(question: QuestionItem, answer: UserAnswer?): String {
    if (answer == null) return "Tidak dijawab"
    return when (answer) {
        is UserAnswer.Pg -> answer.selectedOptionId?.let { "Pilihan $it" } ?: "Tidak dijawab"
        is UserAnswer.PgKompleks -> {
            if (answer.selectedOptionIds.isEmpty()) "Tidak dijawab"
            else "Pilihan ${answer.selectedOptionIds.sorted().joinToString(", ")}"
        }
        is UserAnswer.BenarSalah -> {
            question.statements.mapIndexed { idx, stmt ->
                val label = stmt.key.ifBlank { "P${idx + 1}" }
                val txt = choiceLabel(question, answer.statementSelections[stmt.id])
                "$label: $txt"
            }.joinToString(" · ")
        }
    }
}

private fun formatAnswerKey(question: QuestionItem): String {
    return when (val key = question.answerKey) {
        is AnswerKey.Pg -> key.correctOptionId?.let { "Pilihan $it" } ?: "Belum tersedia"
        is AnswerKey.PgKompleks -> key.correctOptionIds?.takeIf { it.isNotEmpty() }
            ?.let { "Pilihan ${it.sorted().joinToString(", ")}" } ?: "Belum tersedia"
        is AnswerKey.BenarSalah -> {
            val map = key.statementAnswers ?: return "Belum tersedia"
            question.statements.mapIndexed { idx, stmt ->
                val label = stmt.key.ifBlank { "P${idx + 1}" }
                val txt = choiceLabel(question, map[stmt.id])
                "$label: $txt"
            }.joinToString(" · ")
        }
        null -> "Belum tersedia"
    }
}
