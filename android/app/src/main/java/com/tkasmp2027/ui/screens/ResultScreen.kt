package com.tkasmp2027.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.model.ExamReviewStatus
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.QuizEvaluation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    pkg: QuestionPackage,
    evaluation: QuizEvaluation,
    progress: PackageProgress?,
    onReviewAnswers: () -> Unit,
    onViewActivityLog: () -> Unit,
    onBackHome: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
    }
    val violationCount = progress?.violationCount ?: 0
    val reviewStatus = ExamSessionEngine.reviewStatus(violationCount)
    val unansweredCount = (evaluation.totalQuestions - evaluation.answeredCount).coerceAtLeast(0)
    val answeredIncorrectCount = (evaluation.answeredCount - evaluation.correctCount).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBackHome) { Text("Beranda") }
                },
                title = {
                    Text("Hasil Ujian — ${pkg.subjectName} ${pkg.title}")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    Text(
                        text = if (evaluation.keyedCount > 0) "Skor Akhir: ${evaluation.scorePercent}"
                        else "Ujian Selesai (${evaluation.answeredCount}/${evaluation.totalQuestions} dijawab)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Benar: ${evaluation.correctCount} · Salah: $answeredIncorrectCount · Belum Dijawab: $unansweredCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = if (reviewStatus == ExamReviewStatus.NEEDS_SUPERVISOR_REVIEW) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Status Pengawasan: ${reviewStatus.label} ($violationCount pelanggaran)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (reviewStatus == ExamReviewStatus.NEEDS_SUPERVISOR_REVIEW) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    progress?.examStartedAtMillis?.let { startMs ->
                        Text(
                            text = "Mulai: ${dateFormatter.format(Date(startMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    progress?.examCompletedAtMillis?.let { endMs ->
                        Text(
                            text = "Selesai: ${dateFormatter.format(Date(endMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onReviewAnswers) {
                            Text("Tinjau Jawaban")
                        }
                        OutlinedButton(onClick = onViewActivityLog) {
                            Text("Riwayat Aktivitas")
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Rincian per Tipe Soal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    QuestionType.entries.forEach { type ->
                        val breakdown = evaluation.byType[type]
                        if (breakdown != null && breakdown.total > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = type.label, style = MaterialTheme.typography.bodyMedium)
                                val rightText = if (breakdown.keyed > 0) {
                                    "${breakdown.correct}/${breakdown.keyed} benar (${breakdown.answered}/${breakdown.total} dijawab)"
                                } else {
                                    "${breakdown.answered}/${breakdown.total} dijawab"
                                }
                                Text(
                                    text = rightText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
