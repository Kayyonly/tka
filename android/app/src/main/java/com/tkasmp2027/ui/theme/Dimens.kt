package com.tkasmp2027.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared layout metrics. Using one scale keeps spacing consistent across
 * screens so the interface looks designed, not assembled.
 */
object Dimens {
    /** Minimum interactive touch target (accessibility floor). */
    val MinTouch = 48.dp

    val GapXs = 4.dp
    val GapSm = 8.dp
    val GapMd = 12.dp
    val GapLg = 16.dp
    val GapXl = 24.dp

    val ScreenPad = 16.dp
    val CardPad = 16.dp

    /** Height of the progress bar track. */
    val ProgressTrack = 8.dp
    /** Radius used by pill-shaped badges and progress bars. */
    val PillRadius = 999.dp
    /** Leading accent bar width on package/section cards. */
    val AccentBar = 4.dp
}
