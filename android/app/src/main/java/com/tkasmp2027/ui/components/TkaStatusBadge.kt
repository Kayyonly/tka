package com.tkasmp2027.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tkasmp2027.ui.theme.DarkOnSuccessContainer
import com.tkasmp2027.ui.theme.DarkOnWarnContainer
import com.tkasmp2027.ui.theme.DarkSuccessContainer
import com.tkasmp2027.ui.theme.DarkWarnContainer
import com.tkasmp2027.ui.theme.Dimens
import com.tkasmp2027.ui.theme.LightOnSuccessContainer
import com.tkasmp2027.ui.theme.LightOnWarnContainer
import com.tkasmp2027.ui.theme.LightSuccessContainer
import com.tkasmp2027.ui.theme.LightWarnContainer
import com.tkasmp2027.ui.theme.TkaSmpTheme

enum class TkaTone { Neutral, Info, Success, Warn, Danger }

private data class ToneAppearance(
    val container: Color,
    val content: Color,
    val icon: ImageVector
)

/**
 * Always conveys status with BOTH text and an icon — never with color alone.
 * Container/content pairs are selected from the active system's light/dark
 * theme and follow the semantic palette tokens from Phase A.
 */
@Composable
fun TkaStatusBadge(
    text: String,
    tone: TkaTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val dark = isSystemInDarkTheme()
    val appearance = when (tone) {
        TkaTone.Neutral -> ToneAppearance(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Outlined.Info
        )
        TkaTone.Info -> ToneAppearance(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Outlined.Info
        )
        TkaTone.Success -> ToneAppearance(
            if (dark) DarkSuccessContainer else LightSuccessContainer,
            if (dark) DarkOnSuccessContainer else LightOnSuccessContainer,
            Icons.Outlined.CheckCircle
        )
        TkaTone.Warn -> ToneAppearance(
            if (dark) DarkWarnContainer else LightWarnContainer,
            if (dark) DarkOnWarnContainer else LightOnWarnContainer,
            Icons.Outlined.WarningAmber
        )
        TkaTone.Danger -> ToneAppearance(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Outlined.ErrorOutline
        )
    }
    Surface(modifier = modifier, shape = CircleShape, color = appearance.container) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapXs + 2.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon ?: appearance.icon,
                contentDescription = null,
                tint = appearance.content,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = appearance.content
            )
        }
    }
}

@Preview(name = "Badge - Light", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Badge - Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TkaStatusBadgePreview() {
    TkaSmpTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.width(320.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TkaStatusBadge("Belum dimulai", tone = TkaTone.Neutral)
                TkaStatusBadge("Exam Mode aktif", tone = TkaTone.Info)
                TkaStatusBadge("Selesai · Skor 90", tone = TkaTone.Success)
                TkaStatusBadge("Monitoring mode only", tone = TkaTone.Warn)
                TkaStatusBadge("Perlu ditinjau pengawas", tone = TkaTone.Danger)
            }
        }
    }
}
