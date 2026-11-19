package com.ueno.cia.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

enum class DSButtonStyle {
    /** Fondo negro, texto blanco. */
    Primary,
    /** Fondo blanco, borde gris. */
    Secondary,
    /** Transparente con borde negro, chico (ej. "Conocer más" del banner). */
    Tertiary,
    /** Variante de [Secondary] sobre fondos oscuros. */
    SecondaryDark,
    /** [Secondary] chico (pill de 32 dp): "Cambiar QR", "Filtrar". */
    SecondarySmall,
    /** `EGuButtonStyle.accent`: fondo verde, texto oscuro. */
    Accent,
}

private data class ButtonColors(val background: Color, val text: Color, val border: Color?)

private fun colorsFor(style: DSButtonStyle, enabled: Boolean, pressed: Boolean): ButtonColors {
    if (!enabled) return ButtonColors(DSColor.Neutral100, DSColor.Neutral300, null)
    return when (style) {
        DSButtonStyle.Primary -> ButtonColors(if (pressed) DSColor.Black400 else DSColor.Black, DSColor.White, null)
        DSButtonStyle.Secondary, DSButtonStyle.SecondarySmall ->
            ButtonColors(if (pressed) DSColor.Neutral100 else DSColor.White, DSColor.Black, DSColor.Neutral200)
        DSButtonStyle.Tertiary ->
            ButtonColors(if (pressed) DSColor.Black.copy(alpha = 0.06f) else Color.Transparent, DSColor.Black, DSColor.Black)
        DSButtonStyle.SecondaryDark ->
            ButtonColors(if (pressed) DSColor.White.copy(alpha = 0.1f) else Color.Transparent, DSColor.White, DSColor.White)
        DSButtonStyle.Accent -> ButtonColors(if (pressed) DSColor.Green600 else DSColor.Green500, DSColor.Black, null)
    }
}

/** `GuButton`: botones cápsula. */
@Composable
fun DSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DSButtonStyle = DSButtonStyle.Primary,
    enabled: Boolean = true,
    icon: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = colorsFor(style, enabled, pressed)
    val small = style == DSButtonStyle.Tertiary || style == DSButtonStyle.SecondarySmall
    val shape = CircleShape
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (small) 32.dp else 48.dp)
            .clip(shape)
            .background(colors.background, shape)
            .then(if (colors.border != null) Modifier.border(BorderStroke(1.dp, colors.border), shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(
                if (small) PaddingValues(horizontal = Spacing.m, vertical = Spacing.xs)
                else PaddingValues(horizontal = Spacing.xl, vertical = Spacing.s),
            ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) DSIcon(name = icon, tint = colors.text, size = 16.dp)
        DSText(
            text = text,
            style = if (small) DSTextStyle.BodyMStrong else DSTextStyle.BodyLStrong,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
    }
}

/** `GuActionLink`: texto subrayado. */
@Composable
fun DSLinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    DSText(
        text = text,
        style = DSTextStyle.BodyMLink,
        color = if (pressed) DSColor.Neutral600 else DSColor.Black,
        modifier = modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
    )
}

enum class DSIconButtonMode {
    /** Fondo `neutral700` con ícono blanco (sobre headers oscuros). */
    Dark,
    /** Fondo blanco con borde y ícono negro. */
    Light,
}

/** `GuButtonIcon` tamaño small (32 dp). */
@Composable
fun DSIconButton(
    icon: String,
    mode: DSIconButtonMode,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = mode == DSIconButtonMode.Dark
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (dark) DSColor.Neutral700 else DSColor.White, CircleShape)
            .then(if (!dark) Modifier.border(1.dp, DSColor.Neutral100, CircleShape) else Modifier)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        DSIcon(name = icon, tint = if (dark) DSColor.White else DSColor.Black, size = 20.dp)
    }
}
