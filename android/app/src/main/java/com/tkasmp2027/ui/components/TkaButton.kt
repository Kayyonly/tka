package com.tkasmp2027.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tkasmp2027.ui.theme.Dimens
import com.tkasmp2027.ui.theme.TkaSmpTheme

enum class TkaButtonVariant { Primary, Secondary, Ghost, Danger }

/**
 * Shared action button used by every screen in the Study Buddy theme.
 * All colors and shapes resolve from MaterialTheme (including disabled colors),
 * and every interactive target is at least Dimens.MinTouch (48dp).
 */
@Composable
fun TkaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TkaButtonVariant = TkaButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val buttonModifier = modifier.heightIn(min = Dimens.MinTouch)
    val shape = MaterialTheme.shapes.medium
    val padding = if (icon == null) ButtonDefaults.ContentPadding else ButtonDefaults.ButtonWithIconContentPadding
    val content: @Composable () -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }

    when (variant) {
        TkaButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            enabled = enabled,
            contentPadding = padding,
            content = { content() }
        )
        TkaButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            enabled = enabled,
            contentPadding = padding,
            content = { content() }
        )
        TkaButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            enabled = enabled,
            contentPadding = padding,
            content = { content() }
        )
        TkaButtonVariant.Danger -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            contentPadding = padding,
            content = { content() }
        )
    }
}

@Preview(name = "Button - Light", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Button - Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TkaButtonPreview() {
    TkaSmpTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TkaButton("Mulai Ujian", onClick = {}, modifier = Modifier.fillMaxWidth())
                TkaButton(
                    "Lanjutkan",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.CheckCircle
                )
                TkaButton("Tinjau", onClick = {}, variant = TkaButtonVariant.Secondary)
                TkaButton("Riwayat", onClick = {}, variant = TkaButtonVariant.Ghost)
                TkaButton("Reset Sesi", onClick = {}, variant = TkaButtonVariant.Danger)
                TkaButton("Belum Tersedia", onClick = {}, enabled = false)
                TkaButton("Disabled", onClick = {}, variant = TkaButtonVariant.Secondary, enabled = false)
            }
        }
    }
}
