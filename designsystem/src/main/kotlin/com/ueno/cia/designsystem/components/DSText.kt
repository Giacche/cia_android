package com.ueno.cia.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle

/** `GuText`: texto con un estilo tipográfico del design system. */
@Composable
fun DSText(
    text: String,
    style: DSTextStyle,
    modifier: Modifier = Modifier,
    color: Color = DSColor.Black,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.textStyle,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}
