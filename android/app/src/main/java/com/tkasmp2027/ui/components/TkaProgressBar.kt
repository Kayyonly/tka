package com.tkasmp2027.ui.components

import android.content.ContentResolver
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.tkasmp2027.ui.theme.Dimens
import com.tkasmp2027.ui.theme.TkaSmpTheme

private fun animatorDurationScale(resolver: ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

/** True when the OS animator setting permits animation; live and close-safe. */
@Composable
internal fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var scale by remember(resolver) { mutableFloatStateOf(animatorDurationScale(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            private fun update() { scale = animatorDurationScale(resolver) }
            override fun onChange(selfChange: Boolean) { update() }
            override fun onChange(selfChange: Boolean, uri: Uri?) { update() }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale > 0f
}

/**
 * Determinate pill-shaped progress bar with a 220ms ease-out fill.
 * A zero animator scale replaces the tween with snap(), even if the system
 * animation setting is changed while this composable is still on screen.
 * TalkBack receives the numeric progress instead of only a visual color cue.
 */
@Composable
fun TkaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val target = progress.coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animationsEnabled()) tween(220, easing = EaseOut) else snap(),
        label = "TkaProgressBar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ProgressTrack)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(animatedFraction, 0f..1f)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .background(color)
        )
    }
}

@Preview(name = "Progress - Light", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Progress - Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TkaProgressBarPreview() {
    TkaSmpTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Belum mulai — 0%", style = MaterialTheme.typography.bodyMedium)
                TkaProgressBar(progress = 0f)
                Text("Progres — 65%", style = MaterialTheme.typography.bodyMedium)
                TkaProgressBar(progress = 0.65f)
                Text("Selesai — 100%", style = MaterialTheme.typography.bodyMedium)
                TkaProgressBar(progress = 1f)
                Text("Presenter label menjelaskan arti progress.", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
