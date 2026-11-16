package com.tkasmp2027.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Quiet Academic design tokens.
 *
 * Warm ivory/off-white in light mode, warm charcoal/graphite in dark mode,
 * with a sparing muted-copper accent. There are deliberately NO per-subject
 * colors, violet accents, gradients, or decorative color mapping. Component
 * and rich-HTML colors continue to resolve through MaterialTheme.
 *
 * Dark mode remains system-driven only (isSystemInDarkTheme), with no manual
 * toggle or persisted theme preference on Android.
 *
 * Contrast refinement vs the original direction:
 * - light muted #777970 -> #716F68 (safe on white/ivory surface variants)
 * - light copper #A45E46 -> #9D563F (safe on button surfaces AND warm ivory)
 */

// ---------- Light palette: warm ivory and off-white ----------
internal val LightBg = Color(0xFFF5F3EE)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFF5F3EE)
internal val LightOutline = Color(0xFFBFB6AA)
internal val LightOutlineVariant = Color(0xFFDFD9D1)
internal val LightOnSurface = Color(0xFF292C29)
internal val LightOnSurfaceVariant = Color(0xFF716F68)

internal val LightPrimary = Color(0xFF9D563F)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFF1E5DC)
internal val LightOnPrimaryContainer = Color(0xFF4E261C)

internal val LightSecondary = Color(0xFF575950)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFE9E7DF)
internal val LightOnSecondaryContainer = Color(0xFF343730)

internal val LightError = Color(0xFFA23D43)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFF4E4E0)
internal val LightOnErrorContainer = Color(0xFF5D2424)

// Status stays semantically distinct; it is NOT replaced by copper.
internal val LightSuccess = Color(0xFF3F633F)
internal val LightOnSuccess = Color(0xFFFFFFFF)
internal val LightSuccessContainer = Color(0xFFE0EADF)
internal val LightOnSuccessContainer = Color(0xFF264026)
internal val LightWarn = Color(0xFFEAC147)
internal val LightOnWarn = Color(0xFF292C29)
internal val LightWarnContainer = Color(0xFFF2E9D0)
internal val LightOnWarnContainer = Color(0xFF594210)

// ---------- Dark palette: warm charcoal and graphite ----------
internal val DarkBg = Color(0xFF191A18)
internal val DarkSurface = Color(0xFF232522)
internal val DarkSurfaceVariant = Color(0xFF30332D)
internal val DarkOutline = Color(0xFF767466)
internal val DarkOutlineVariant = Color(0xFF3C3E37)
internal val DarkOnSurface = Color(0xFFF0EEE8)
internal val DarkOnSurfaceVariant = Color(0xFFB2B0A7)

internal val DarkPrimary = Color(0xFFC68B72)
internal val DarkOnPrimary = Color(0xFF302017)
internal val DarkPrimaryContainer = Color(0xFF47342C)
internal val DarkOnPrimaryContainer = Color(0xFFF0D7C9)

internal val DarkSecondary = Color(0xFFB0B3A7)
internal val DarkOnSecondary = Color(0xFF242723)
internal val DarkSecondaryContainer = Color(0xFF33372F)
internal val DarkOnSecondaryContainer = Color(0xFFE5E8DB)

internal val DarkError = Color(0xFFDFA39F)
internal val DarkOnError = Color(0xFF3D1715)
internal val DarkErrorContainer = Color(0xFF462927)
internal val DarkOnErrorContainer = Color(0xFFF4DAD6)

internal val DarkSuccess = Color(0xFF9BB795)
internal val DarkOnSuccess = Color(0xFF203121)
internal val DarkSuccessContainer = Color(0xFF2C392B)
internal val DarkOnSuccessContainer = Color(0xFFDAE5D4)
internal val DarkWarn = Color(0xFFD4B66D)
internal val DarkOnWarn = Color(0xFF39270A)
internal val DarkWarnContainer = Color(0xFF3C3423)
internal val DarkOnWarnContainer = Color(0xFFF4E9C6)


