package com.tkasmp2027.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tkasmp2027.ui.components.*
import com.tkasmp2027.ui.theme.TkaSmpTheme
import kotlinx.coroutines.delay

/** Isolated debug-only gallery; no production screen/state is used or modified. */
class ComponentPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TkaSmpTheme { ComponentPreviewGallery() }
        }
    }
}

@Composable
private fun ComponentPreviewGallery() {
    var section by remember { mutableIntStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("DEBUG · KOMPONEN DASAR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("Light + Dark (sistem)", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TkaButton(
                    "B1 Button",
                    onClick = { section = 0 },
                    modifier = Modifier.weight(1f),
                    variant = if (section == 0) TkaButtonVariant.Primary else TkaButtonVariant.Secondary
                )
                TkaButton(
                    "B2 Card",
                    onClick = { section = 1 },
                    modifier = Modifier.weight(1f),
                    variant = if (section == 1) TkaButtonVariant.Primary else TkaButtonVariant.Secondary
                )
                TkaButton(
                    "B3 Status",
                    onClick = { section = 2 },
                    modifier = Modifier.weight(1f),
                    variant = if (section == 2) TkaButtonVariant.Primary else TkaButtonVariant.Secondary
                )
            }
            Column(
                modifier = Modifier.padding(top = 20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (section) {
                    0 -> ButtonsSection()
                    1 -> CardsSection()
                    else -> BadgesProgressSection()
                }
            }
        }
    }
}

@Composable
private fun CardsSection() {
    TkaSectionHeader("Matematika", subtitle = "Aljabar, Geometri, Bilangan, Statistika & Peluang", icon = Icons.Outlined.Calculate)
    TkaCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Text("Matematika — Paket 1", style = MaterialTheme.typography.titleMedium)
        Text("30 soal · proses 65%", style = MaterialTheme.typography.bodyMedium)
        TkaProgressBar(progress = 0.65f)
    }
    TkaSectionHeader("Bahasa Indonesia", subtitle = "Pemahaman bacaan dan kebahasaan", icon = Icons.AutoMirrored.Outlined.MenuBook)
    TkaCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        Text("Bahasa Indonesia — Paket 1", style = MaterialTheme.typography.titleMedium)
        Text("Body sample untuk readability.", style = MaterialTheme.typography.bodyLarge)
        Text("Skor 86", style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
private fun BadgesProgressSection() {
    TkaStatusBadge("Belum dimulai", tone = TkaTone.Neutral)
    TkaStatusBadge("Exam Mode aktif", tone = TkaTone.Info)
    TkaStatusBadge("Selesai · Skor 90", tone = TkaTone.Success)
    TkaStatusBadge("Monitoring mode only", tone = TkaTone.Warn)
    TkaStatusBadge("Perlu ditinjau pengawas", tone = TkaTone.Danger)
    Text("Animator OS aktif: ${animationsEnabled()}", style = MaterialTheme.typography.labelMedium)
    var progress by remember { mutableFloatStateOf(0.25f) }
    LaunchedEffect(Unit) {
        while (true) {
            progress = 0.25f
            delay(2000)
            progress = 0.85f
            delay(2000)
        }
    }
    Text("Animasi progress: ${"%.0f".format(progress * 100)}%")
    TkaProgressBar(progress)
    TkaProgressBar(progress = 0f, color = MaterialTheme.colorScheme.secondary)
    TkaProgressBar(progress = 1f, color = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun ButtonsSection() {
    TkaButton("Mulai Ujian", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.CheckCircle)
    TkaButton("Tinjau", onClick = {}, modifier = Modifier.fillMaxWidth(), variant = TkaButtonVariant.Secondary)
    TkaButton("Riwayat", onClick = {}, modifier = Modifier.fillMaxWidth(), variant = TkaButtonVariant.Ghost)
    TkaButton("Reset Sesi", onClick = {}, modifier = Modifier.fillMaxWidth(), variant = TkaButtonVariant.Danger)
    TkaButton("Belum Tersedia", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
    TkaButton("Disabled Secondary", onClick = {}, variant = TkaButtonVariant.Secondary, enabled = false)
}
