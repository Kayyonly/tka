package com.tkasmp2027.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale for the Quiet Academic theme.
 *
 * Larger radii than Material defaults give a soft, friendly feel without
 * becoming childish. Existing screens reference MaterialTheme.shapes.small /
 * medium / large, so adopting these shapes restyles the whole UI consistently.
 */
val TkaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
