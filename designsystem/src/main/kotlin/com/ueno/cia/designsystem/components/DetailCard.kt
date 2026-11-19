package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

/** `GuCardData` (detalle): tarjeta blanca con título, divisor y filas. */
@Composable
fun DetailCard(title: String, modifier: Modifier = Modifier, rows: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Radius.s)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dsShadow(DSShadow.M, shape)
            .background(DSColor.White, shape)
            .padding(start = Spacing.l, end = Spacing.l, top = Spacing.l, bottom = Spacing.m),
    ) {
        DSText(title, DSTextStyle.BodyLStrong)
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.s))
        Box(Modifier.fillMaxWidth().height(1.dp).background(DSColor.Neutral200))
        rows()
    }
}

/** `GuListItem.double`: etiqueta gris arriba y valor abajo; a la derecha, una acción opcional ([trailing]). */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: Pair<String, String>? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "$label, $value" },
            verticalArrangement = Arrangement.spacedBy(Spacing.x3s),
        ) {
            DSText(label, DSTextStyle.BodyM, color = DSColor.Neutral500)
            DSText(value, DSTextStyle.BodyL)
        }
        if (badge != null) DSBadge(type = badge.first, text = badge.second)
        trailing?.invoke()
    }
}
