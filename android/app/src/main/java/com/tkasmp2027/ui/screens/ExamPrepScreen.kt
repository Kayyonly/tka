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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.model.LockModeStatus
import com.tkasmp2027.model.QuestionPackage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPrepScreen(
    pkg: QuestionPackage,
    lockModeStatus: LockModeStatus,
    onConfirmStartExam: () -> Unit,
    onCancelBack: () -> Unit
) {
    val durationMinutes = ExamSessionEngine.defaultDurationSeconds(pkg.questions.size) / 60

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onCancelBack) {
                        Text("Kembali")
                    }
                },
                title = {
                    Text(
                        text = "Persiapan Ujian",
                        fontWeight = FontWeight.Bold
                    )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "${pkg.subjectName} — ${pkg.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pkg.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jumlah Soal", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${pkg.questions.size} Soal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Durasi Ujian", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$durationMinutes Menit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Status Pengamanan Perangkat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = if (lockModeStatus == LockModeStatus.FULL_LOCKDOWN_AVAILABLE) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = lockModeStatus.badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = if (lockModeStatus == LockModeStatus.FULL_LOCKDOWN_AVAILABLE) {
                            "Perangkat telah dikonfigurasi oleh Device Policy Controller (DPC). Android Lock Task Mode dan perlindungan layar akan diaktifkan saat ujian dimulai."
                        } else {
                            "Perangkat belum dikonfigurasi DPC untuk penguncian penuh (Lock Task Mode). Ujian tetap berjalan dengan perlindungan layar (FLAG_SECURE) dan pencatatan aktivitas saat aplikasi meninggalkan foreground."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Petunjuk Pengerjaan & Tata Tertib",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. Jawaban disimpan otomatis setiap kali Anda memilih opsi.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Timer berjalan berdasarkan waktu nyata perangkat dan sisa waktu dipulihkan jika aplikasi dibuka ulang.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Navigasi keluar dan tombol Back dinonaktifkan selama ujian berlangsung.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "4. Aktivitas meninggalkan aplikasi akan dicatat dalam riwayat pengawas. Ujian yang sudah dimulai atau dikumpulkan tidak dapat diulang peserta untuk menghapus catatan aktivitas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = onConfirmStartExam,
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Konfirmasi & Mulai Ujian")
                }
            }
        }
    }
}
