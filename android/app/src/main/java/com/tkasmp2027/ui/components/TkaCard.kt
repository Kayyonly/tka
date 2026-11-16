package com.tkasmp2027.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tkasmp2027.ui.theme.Dimens
import com.tkasmp2027.ui.theme.TkaSmpTheme

/**
 * Surface card consistent with the application theme.
 * The optional leading accent is only a slim solid color bar (no gradient,
 * blur, or expensive effect) so it stays performant on minSdk 24 devices.
 * Content keeps using the composable-supplied surfaceColor and typography.
 */
@Composable
fun TkaCard(
    modifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    accentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .width(Dimens.AccentBar)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(Dimens.CardPad),
                content = content
            )
        }
    }
}

@Preview(name = "Card - Light", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Card - Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TkaCardPreview() {
    TkaSmpTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TkaCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Matematika — Referensi · Paket 1", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "30 Soal · Selesai · Skor 0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("90%", style = MaterialTheme.typography.labelLarge)
                }
                TkaCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Bahasa Indonesia — Paket 1", style = MaterialTheme.typography.titleMedium)
                    Text("Kartu tanpa dekorasi berlebihan.", style = MaterialTheme.typography.bodyMedium)
                }
                TkaCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Kartu netral tanpa aksen", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
