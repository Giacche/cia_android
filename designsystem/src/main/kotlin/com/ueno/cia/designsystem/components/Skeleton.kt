package com.ueno.cia.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.Radius

enum class SkeletonMode { Light, Dark }

/** `GuSkeleton`: bloque gris con shimmer. Sin [width] ocupa todo el ancho disponible. */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    circle: Boolean = false,
    mode: SkeletonMode = SkeletonMode.Light,
) {
    val base = if (mode == SkeletonMode.Light) DSColor.Neutral100 else DSColor.Neutral700
    val highlight = if (mode == SkeletonMode.Light) DSColor.Neutral50 else DSColor.Neutral600
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton_progress",
    )
    val shape = if (circle) CircleShape else RoundedCornerShape(Radius.x2s)
    val size = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    // El brillo recorre el bloque de izquierda a derecha (1200 ms)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 400f, 0f),
        end = Offset(progress * 400f + 400f, 0f),
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier.then(size).height(height).clip(shape).background(brush),
    )
}
