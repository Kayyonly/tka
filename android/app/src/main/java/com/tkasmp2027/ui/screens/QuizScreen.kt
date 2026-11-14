package com.tkasmp2027.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.engine.QuizScoringEngine
import com.tkasmp2027.model.LockModeStatus
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuestionStatementChoice
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.UserAnswer
import com.tkasmp2027.ui.components.RichHtmlContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    pkg: QuestionPackage,
    currentIndex: Int,
    answers: Map<String, UserAnswer>,
    remainingSeconds: Int,
    lockModeStatus: LockModeStatus,
    violationCount: Int,
    warningBannerMessage: String?,
    onDismissWarning: () -> Unit,
    onIndexChange: (Int) -> Unit,
    onUpdateAnswer: (String, UserAnswer) -> Unit,
    onAttemptExitNavigation: () -> Unit,
    onSubmitQuiz: () -> Unit
) {
    // Prevent leaving Exam Mode via Android system Back button; record attempt
    BackHandler(enabled = true) {
        onAttemptExitNavigation()
    }

    val questions = pkg.questions
    val safeIndex = currentIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
    val question = questions[safeIndex]
    val currentAnswer = answers[question.id]
    val answeredCount = questions.count { QuizScoringEngine.isQuestionAnswered(it, answers[it.id]) }

    var showNavigator by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    val formattedTimer = ExamSessionEngine.formatTimer(remainingSeconds)
    val isLowTime = remainingSeconds in 1..300

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${pkg.subjectName} — ${pkg.title}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Soal ${safeIndex + 1} / ${questions.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        Surface(
                            color = if (isLowTime) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = formattedTimer,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isLowTime) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        TextButton(onClick = { showNavigator = true }) {
                            Text("Daftar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                LinearProgressIndicator(
                    progress = {
                        if (questions.isNotEmpty()) answeredCount.toFloat() / questions.size.toFloat() else 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXAM MODE ACTIVE · ${lockModeStatus.badgeText}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pelanggaran: $violationCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (violationCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onIndexChange(safeIndex - 1) },
                        enabled = safeIndex > 0
                    ) {
                        Text("Sebelumnya")
                    }

                    TextButton(onClick = { showSubmitConfirm = true }) {
                        Text("Kumpulkan ($answeredCount/${questions.size})")
                    }

                    if (safeIndex == questions.lastIndex) {
                        Button(onClick = { showSubmitConfirm = true }) {
                            Text("Kumpulkan")
                        }
                    } else {
                        Button(onClick = { onIndexChange(safeIndex + 1) }) {
                            Text("Selanjutnya")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!warningBannerMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WARNING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = warningBannerMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(onClick = onDismissWarning) {
                            Text("Tutup")
                        }
                    }
                }
            }

            Text(
                text = "Nomor ${question.number} · ID: ${question.id} · ${question.type.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (!question.stimulusHtml.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    RichHtmlContent(
                        html = question.stimulusHtml,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (question.image != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        RichHtmlContent(
                            html = "<img src=\"${question.image.url}\" alt=\"${question.image.alt}\" />"
                        )
                        if (!question.image.caption.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = question.image.caption,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RichHtmlContent(
                        html = question.questionHtml,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    if (!question.mathFormula.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question.mathFormula,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    when (question.type) {
                        QuestionType.PG -> {
                            val selectedId = (currentAnswer as? UserAnswer.Pg)?.selectedOptionId
                            question.options.forEach { option ->
                                val isSelected = selectedId == option.id
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdateAnswer(question.id, UserAnswer.Pg(option.id))
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                onUpdateAnswer(question.id, UserAnswer.Pg(option.id))
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${option.label}. ",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        RichHtmlContent(
                                            html = option.html,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        QuestionType.PG_KOMPLEKS -> {
                            val selectedSet =
                                (currentAnswer as? UserAnswer.PgKompleks)?.selectedOptionIds.orEmpty()
                            Text(
                                text = "Pilih lebih dari satu jawaban (MCMA):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            question.options.forEach { option ->
                                val isChecked = selectedSet.contains(option.id)
                                val toggleAction = {
                                    val next = if (isChecked) selectedSet - option.id else selectedSet + option.id
                                    onUpdateAnswer(question.id, UserAnswer.PgKompleks(next))
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isChecked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { toggleAction() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { toggleAction() }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${option.label}. ",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        RichHtmlContent(
                                            html = option.html,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        QuestionType.BENAR_SALAH -> {
                            val selections =
                                (currentAnswer as? UserAnswer.BenarSalah)?.statementSelections.orEmpty()
                            val choices = question.statementChoices.ifEmpty {
                                listOf(
                                    QuestionStatementChoice(1, 1, "Benar"),
                                    QuestionStatementChoice(2, 2, "Salah")
                                )
                            }
                            Text(
                                text = "Tentukan ${choices.joinToString(" atau ") { it.label }} untuk setiap pernyataan:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            question.statements.forEachIndexed { idx, stmt ->
                                val selectedOrder = selections[stmt.id]
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stmt.label ?: "Pernyataan ${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        RichHtmlContent(html = stmt.html)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            choices.forEach { choice ->
                                                val isSelected = selectedOrder == choice.order
                                                OutlinedButton(
                                                    onClick = {
                                                        val next = selections.toMutableMap().apply {
                                                            put(stmt.id, choice.order)
                                                        }
                                                        onUpdateAnswer(question.id, UserAnswer.BenarSalah(next))
                                                    },
                                                    colors = if (isSelected) {
                                                        ButtonDefaults.buttonColors()
                                                    } else {
                                                        ButtonDefaults.outlinedButtonColors()
                                                    }
                                                ) {
                                                    Text(choice.label)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNavigator) {
        AlertDialog(
            onDismissRequest = { showNavigator = false },
            title = { Text("Daftar Soal (${answeredCount}/${questions.size})") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(260.dp)
                ) {
                    items(questions.size) { idx ->
                        val q = questions[idx]
                        val answered = QuizScoringEngine.isQuestionAnswered(q, answers[q.id])
                        val isCurrent = idx == safeIndex
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                answered -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.clickable {
                                onIndexChange(idx)
                                showNavigator = false
                            }
                        ) {
                            Text(
                                text = "${q.number}",
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showNavigator = false
                    showSubmitConfirm = true
                }) {
                    Text("Kumpulkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNavigator = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text("Kumpulkan Jawaban Ujian?") },
            text = {
                Text(
                    "Anda telah menjawab $answeredCount dari ${questions.size} soal. Setelah dikumpulkan, Exam Mode berakhir dan jawaban tidak dapat diubah kembali."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitConfirm = false
                    onSubmitQuiz()
                }) {
                    Text("Ya, Kumpulkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = false }) {
                    Text("Periksa Kembali")
                }
            }
        )
    }
}
