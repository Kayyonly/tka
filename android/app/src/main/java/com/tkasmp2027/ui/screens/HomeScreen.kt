package com.tkasmp2027.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.SubjectId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    packages: List<QuestionPackage>,
    progressMap: Map<String, PackageProgress>,
    onStartPackage: (String) -> Unit,
    onReviewPackage: (String) -> Unit,
    onResetProgress: (String) -> Unit
) {
    val grouped = packages.groupBy { it.subjectId }
    val completedCount = packages.count { progressMap[it.packageId]?.status == ProgressStatus.COMPLETED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TKA SMP 2027",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Persiapan TKA SMP 2027",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mendukung Pilihan Ganda, PG Kompleks (MCMA), dan Benar/Salah. Progres tersimpan lokal ($completedCount/${packages.size} paket selesai).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SubjectId.entries.forEach { subject ->
                val subjectPackages = grouped[subject].orEmpty()
                item(key = "header-${subject.key}") {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = subject.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subject.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(subjectPackages, key = { it.packageId }) { pkg ->
                    val progress = progressMap[pkg.packageId]
                    val total = pkg.questions.size
                    val answered = progress?.lastAnsweredCount ?: 0
                    val ratio = if (total > 0) answered.toFloat() / total.toFloat() else 0f
                    val statusLabel = when {
                        total == 0 -> "Belum tersedia"
                        progress?.status == ProgressStatus.COMPLETED -> {
                            val score = progress.lastScorePercent
                            if (progress.lastKeyedCount > 0 && score != null) {
                                "Selesai · Skor $score"
                            } else {
                                "Selesai · $answered/$total dijawab"
                            }
                        }
                        progress?.status == ProgressStatus.IN_PROGRESS -> "Berlangsung · $answered/$total dijawab"
                        else -> "Belum dimulai"
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
                            Column {
                                Text(
                                    text = "${pkg.subjectName} — ${pkg.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$total Soal · $statusLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = pkg.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (total > 0) {
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    enabled = total > 0,
                                    onClick = { onStartPackage(pkg.packageId) }
                                ) {
                                    val label = when {
                                        total == 0 -> "Belum Tersedia"
                                        progress?.status == ProgressStatus.IN_PROGRESS -> "Lanjutkan"
                                        progress?.status == ProgressStatus.COMPLETED -> "Kerjakan Lagi"
                                        else -> "Mulai Latihan"
                                    }
                                    Text(label)
                                }

                                if (total > 0 && progress?.status == ProgressStatus.COMPLETED) {
                                    OutlinedButton(onClick = { onReviewPackage(pkg.packageId) }) {
                                        Text("Tinjau")
                                    }
                                }

                                if (total > 0 && progress != null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { onResetProgress(pkg.packageId) }) {
                                        Text("Reset")
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
