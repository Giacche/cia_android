package com.ueno.cia.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSFlag
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.images.DSIllustration
import com.ueno.cia.designsystem.images.hasDSIcon
import com.ueno.cia.designsystem.images.rememberFlagPath
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSSize
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

/** `ShadowFoundation` (sombras del tema ueno), aproximadas con la elevación de Compose. */
enum class DSShadow(val elevation: androidx.compose.ui.unit.Dp) { S(2.dp), M(4.dp), L(8.dp) }

fun Modifier.dsShadow(shadow: DSShadow, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.shadow(shadow.elevation, shape, clip = false, ambientColor = DSColor.Black, spotColor = DSColor.Black)

// MARK: - QuickAction + grilla

/** `GuQuickAction` tamaño M: card blanca con ícono en círculo y título. */
@Composable
fun QuickActionCard(
    title: String,
    description: String?,
    icon: String,
    disabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.xs)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spoken = listOfNotNull(title, description?.takeIf { it.isNotEmpty() }).joinToString(", ")
    Column(
        modifier = modifier
            .then(if (disabled) Modifier.border(BorderStroke(1.dp, DSColor.Neutral100), shape) else Modifier.dsShadow(DSShadow.M, shape))
            .clip(shape)
            .background(if (pressed) DSColor.Neutral200 else DSColor.White, shape)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                if (disabled) disabled()
            }
            .clickable(interactionSource = interaction, indication = null, enabled = !disabled, role = Role.Button, onClick = onClick)
            .padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(DSSize.xl)
                .background(if (disabled) DSColor.Neutral100 else DSColor.Blue100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val flag = rememberFlagPath(icon)
            when {
                // `QrFlagIcon`: bandera circular de 22 dp, sin tinte
                flag != null -> DSFlag(path = flag, size = 22.dp)
                else -> DSIcon(
                    // Flutter muestra `Icons.add` cuando el ícono del BFF no existe en el design system
                    name = if (hasDSIcon(icon)) icon else "plus",
                    tint = if (disabled) DSColor.Neutral400 else DSColor.Blue800,
                    size = 20.dp,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
            DSText(title, DSTextStyle.BodyMStrong, color = if (disabled) DSColor.Neutral300 else DSColor.Black, maxLines = 1)
            if (!description.isNullOrEmpty()) {
                DSText(description, DSTextStyle.BodyM, color = if (disabled) DSColor.Neutral300 else DSColor.Neutral500, maxLines = 2)
            }
        }
    }
}

/** Grilla de dos columnas con alto derivado del ancho (`aspectRatio` = ancho / alto). */
@Composable
fun TwoColumnGrid(items: List<@Composable (Modifier) -> Unit>, aspectRatio: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item -> item(Modifier.weight(1f).aspectRatio(aspectRatio)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// MARK: - Sección

/**
 * `GuSection` sin padding vertical: título (displayS) + link opcional + 24 dp + contenido, con márgenes de 20 dp.
 * `padsContent = false` deja el contenido a ancho completo (carruseles que manejan su propio margen).
 */
@Composable
fun DSSection(
    title: String?,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    /** `testTag` del link de la derecha (el BFF lo manda en `linkText.tag`). */
    actionTag: String? = null,
    padsContent: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        if (title != null || actionTitle != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DSText(title.orEmpty(), DSTextStyle.DisplayS, modifier = Modifier.weight(1f), maxLines = 1)
                if (actionTitle != null && onAction != null) {
                    DSLinkButton(actionTitle, onAction, modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier)
                }
            }
        }
        Box(modifier = if (padsContent) Modifier.padding(horizontal = Spacing.l) else Modifier) { content() }
    }
}

// MARK: - Inline message

enum class InlineMessageState(
    val background: Color,
    val border: Color,
    val iconBackground: Color,
    val icon: String,
) {
    Information(DSColor.Blue50, DSColor.Blue800, DSColor.Blue500, "information"),
    Success(DSColor.Green50, DSColor.Green800, DSColor.Green500, "check"),
    Warning(DSColor.Yellow50, DSColor.Yellow800, DSColor.Yellow500, "warning"),
    Error(DSColor.Pink50, DSColor.Pink800, DSColor.Pink500, "warning"),
    ;

    companion object {
        /** Equivale a `InlineMessageStateHelper.getByName`. */
        fun byName(name: String?): InlineMessageState = when (name?.lowercase()) {
            "success" -> Success
            "warning" -> Warning
            "error" -> Error
            else -> Information
        }
    }
}

/** `GuInlineMessage` tipo `empty`: caja con borde, ícono de estado, título, descripción y link opcional. */
@Composable
fun InlineMessage(
    title: String?,
    description: String,
    state: InlineMessageState,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    /** `GuInlineMessage.onClose`: muestra una X a la derecha. */
    onClose: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Radius.xs)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(state.background, shape)
            .border(1.dp, state.border, shape)
            .padding(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Box(
            modifier = Modifier.size(Spacing.l).background(state.iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            DSIcon(state.icon, tint = DSColor.White, size = 12.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2s), modifier = Modifier.weight(1f)) {
            if (!title.isNullOrEmpty()) DSText(title, DSTextStyle.BodyMStrong)
            DSText(description, DSTextStyle.BodyM)
            if (actionTitle != null && onAction != null) {
                Spacer(Modifier.height(Spacing.x2s))
                DSLinkButton(actionTitle, onAction)
            }
        }
        if (onClose != null) {
            Box(
                Modifier
                    .size(Spacing.l)
                    .semantics { contentDescription = "Cerrar" }
                    .clickable(role = Role.Button, onClick = onClose)
                    .testTag("cia_inline_message_close"),
                contentAlignment = Alignment.Center,
            ) { DSIcon("close", tint = DSColor.Black, size = 16.dp) }
        }
    }
}

// MARK: - Banner

enum class OverviewBannerColor(val background: Color, val overline: Color) {
    Blue(DSColor.Blue200, DSColor.Blue800),
    Green(DSColor.Green200, DSColor.Green800),
    Violet(DSColor.Violet200, DSColor.Violet800),
    Black(DSColor.Black, DSColor.Neutral400),
    ;

    companion object {
        fun byName(name: String): OverviewBannerColor = when (name.lowercase()) {
            "green" -> Green
            "violet" -> Violet
            "black" -> Black
            else -> Blue
        }
    }
}

val OverviewBannerHeight = 150.dp

/** `GuOverviewBanner` tamaño M (alto 150): overline, título, botón terciario e ilustración a la derecha. */
@Composable
fun OverviewBanner(
    overline: String,
    title: String,
    buttonText: String,
    color: OverviewBannerColor,
    illustration: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.xs)
    val dark = color == OverviewBannerColor.Black
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(OverviewBannerHeight)
            .clip(shape)
            .background(color.background, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
            DSText(overline.uppercase(), DSTextStyle.OverlineM, color = color.overline, maxLines = 1)
            DSText(title, DSTextStyle.DisplayXS, color = if (dark) DSColor.White else DSColor.Black, maxLines = 3)
            Spacer(Modifier.weight(1f))
            DSButton(
                text = buttonText,
                onClick = onClick,
                style = if (dark) DSButtonStyle.SecondaryDark else DSButtonStyle.Tertiary,
            )
        }
        DSIllustration(name = illustration, modifier = Modifier.width(112.dp).fillMaxHeight())
    }
}

// MARK: - Movimientos, estados

/** Fila de movimiento: título/subtítulo a la izquierda, importe/fecha a la derecha. */
@Composable
fun MovementRow(title: String, subtitle: String, amount: String, date: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (pressed) 0.5f else 1f)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $subtitle, $amount, $date" }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            DSText(title, DSTextStyle.BodyM, maxLines = 1)
            DSText(subtitle, DSTextStyle.BodyM, color = DSColor.Neutral500, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            DSText(amount, DSTextStyle.BodyM, textAlign = TextAlign.End, maxLines = 1)
            DSText(date, DSTextStyle.BodyM, color = DSColor.Neutral500, textAlign = TextAlign.End, maxLines = 1)
        }
    }
}

/** Estado vacío de listas (ilustración + título + subtítulo). */
@Composable
fun EmptyState(illustration: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.x4l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x2s),
    ) {
        DSIllustration(illustration, modifier = Modifier.height(80.dp))
        Spacer(Modifier.height(Spacing.xs))
        DSText(title, DSTextStyle.BodyLStrong, textAlign = TextAlign.Center)
        if (subtitle.isNotEmpty()) DSText(subtitle, DSTextStyle.BodyM, textAlign = TextAlign.Center)
    }
}

/** `GuCardData`: card blanca con título, divisor y contenido. */
@Composable
fun DataCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Radius.xs)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dsShadow(DSShadow.S, shape)
            .background(DSColor.White, shape)
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        DSText(title, DSTextStyle.BodyLStrong)
        Box(Modifier.fillMaxWidth().height(1.dp).background(DSColor.Neutral100))
        content()
    }
}
