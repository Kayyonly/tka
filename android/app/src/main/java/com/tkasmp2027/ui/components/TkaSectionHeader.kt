package com.tkasmp2027.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tkasmp2027.ui.theme.Dimens
import com.tkasmp2027.ui.theme.TkaSmpTheme

/**
 * Reusable section title. The icon is decorative: the title/subtitle carries
 * the accessible meaning, so it never needs a contentDescription of its own.
 */
@Composable
fun TkaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapXs)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Section - Light", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Section - Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TkaSectionHeaderPreview() {
    TkaSmpTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TkaSectionHeader(
                    title = "Matematika",
                    subtitle = "Aljabar, Geometri, Bilangan, Statistika & Peluang",
                    icon = Icons.Outlined.Calculate
                )
                TkaSectionHeader(
                    title = "Bahasa Indonesia",
                    subtitle = "Pemahaman Bacaan, Literasi Informasi & Kebahasaan",
                    icon = Icons.AutoMirrored.Outlined.MenuBook
                )
                TkaSectionHeader(title = "Judul saja tanpa ikon")
            }
        }
    }
}
