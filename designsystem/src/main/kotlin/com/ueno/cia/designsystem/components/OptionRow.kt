package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.images.hasDSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSSize
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** `GuListItem.double(invert: true)` con miniatura a la izquierda y chevron a la derecha (selector de proveedor). */
@Composable
fun OptionRow(icon: String, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spoken = listOf(title, subtitle).filter { it.isNotEmpty() }.joinToString(", ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (pressed) 0.5f else 1f)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(DSSize.xl).background(DSColor.Blue100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Un ícono que no existe en el design system se muestra como `plus` (igual que iOS)
            DSIcon(if (hasDSIcon(icon)) icon else "plus", tint = DSColor.Blue800, size = 20.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x3s)) {
            DSText(title, DSTextStyle.BodyLStrong, maxLines = 1)
            if (subtitle.isNotEmpty()) DSText(subtitle, DSTextStyle.BodyM, color = DSColor.Neutral500, maxLines = 2)
        }
        DSIcon("chevronRight", tint = DSColor.Black, size = 20.dp)
    }
}
