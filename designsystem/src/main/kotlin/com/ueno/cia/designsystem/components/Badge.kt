package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

private fun badgeColors(type: String): Pair<Color, Color> = when (type.lowercase()) {
    "success" -> DSColor.Green200 to DSColor.Green800
    "warning" -> DSColor.Yellow50 to DSColor.Yellow800
    "error" -> DSColor.Pink50 to DSColor.Pink800
    "info" -> DSColor.Blue100 to DSColor.Blue800
    else -> DSColor.Neutral100 to DSColor.Neutral700
}

/** `GuTextBadge`: pill con texto; el estilo viene del `type` del BFF (`success`, `warning`, `error`, `info`; otro → neutral). */
@Composable
fun DSBadge(type: String, text: String, modifier: Modifier = Modifier) {
    val (background, foreground) = badgeColors(type)
    DSText(
        text = text,
        style = DSTextStyle.BodyMStrong,
        color = foreground,
        maxLines = 1,
        modifier = modifier
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = Spacing.xs, vertical = Spacing.x3s)
            .semantics { contentDescription = text },
    )
}
