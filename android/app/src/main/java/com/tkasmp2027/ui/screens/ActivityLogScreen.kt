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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.model.ExamReviewStatus
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.QuestionPackage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    pkg: QuestionPackage,
    progress: PackageProgress?,
    onBack: () -> Unit,
    onProctorResetSession: () -> Unit
) {
    var showProctorDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
    }
    val violationCount = progress?.violationCount ?: 0
    val reviewStatus = ExamSessionEngine.reviewStatus(violationCount)
    val events = progress?.activityLog.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Kembali")
                    }
                },
                title = {
                    Text("Riwayat Aktivitas Ujian")
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
            item {
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
                            text = "${pkg.subjectName} — ${pkg.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
                                text = "Status Pengawasan: ${reviewStatus.label}",
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
                        Text(
                            text = "Jumlah Pelanggaran Tercatat: $violationCount (Batas tinjauan pengawas: ${ExamSessionEngine.SUPERVISOR_REVIEW_THRESHOLD})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Mode Keamanan: ${progress?.lockModeStatus?.badgeText ?: "MONITORING MODE ONLY"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        progress?.examStartedAtMillis?.let { startMs ->
                            Text(
                                text = "Waktu Mulai: ${dateFormatter.format(Date(startMs))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        progress?.examCompletedAtMillis?.let { endMs ->
                            Text(
                                text = "Waktu Selesai: ${dateFormatter.format(Date(endMs))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (progress != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    pinInput = ""
                                    pinError = null
                                    showProctorDialog = true
                                }
                            ) {
                                Text("Prosedur Pengawas: Buka Ulang Sesi")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Log Kejadian (${events.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (events.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Belum ada aktivitas pelanggaran atau pengumpulan yang tercatat pada sesi ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(events.reversed()) { event ->
                    val isViolation = ExamSessionEngine.isViolationEvent(event.type)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isViolation) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = event.type.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isViolation) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = dateFormatter.format(Date(event.timestampMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = event.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProctorDialog) {
        AlertDialog(
            onDismissRequest = { showProctorDialog = false },
            title = { Text("Prosedur Resmi Pengawas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hanya pengawas/guru yang berwenang membuka ulang ujian yang telah dikumpulkan atau mereset riwayat sesi.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = null
                        },
                        label = { Text("Kode Pengawas") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError != null
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.trim() == ExamSessionEngine.PROCTOR_RESET_PIN) {
                        showProctorDialog = false
                        onProctorResetSession()
                    } else {
                        pinError = "Kode pengawas tidak valid."
                    }
                }) {
                    Text("Reset Sesi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProctorDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
