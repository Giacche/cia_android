package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

/** `SelectInputSingleWrapper`: etiqueta, caja con el valor (o la pista en gris) y chevron. Al tocarla se abre un selector. */
@Composable
fun DSSelectField(
    label: String,
    value: String?,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    requiredMark: Boolean = false,
) {
    val shape = RoundedCornerShape(Radius.xs)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = value ?: hint
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        DSText(if (requiredMark) "$label *" else label, DSTextStyle.BodyLStrong)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(DSColor.White, shape)
                .border(1.dp, DSColor.Neutral200, shape)
                .padding(horizontal = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            DSText(
                text = value ?: hint,
                style = DSTextStyle.BodyL,
                color = if (value == null) DSColor.Neutral300 else DSColor.Black,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            DSIcon("chevronDown", tint = DSColor.Black, size = 20.dp)
        }
    }
}

/** Lista de opciones con botón de radio dentro de un bottom sheet (`SelectInputSingleWrapperModalContent`). */
@Composable
fun RadioOptionsSheet(
    title: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DSBottomSheet(title = title, onDismiss = onDismiss) { dismiss ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
            options.forEach { (id, name) ->
                val selected = id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { this.selected = selected }
                        .clickable(role = Role.RadioButton) { dismiss { onSelect(id) } }
                        .padding(vertical = Spacing.x2s)
                        .testTag("cia_option_$id"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    Box(
                        modifier = Modifier.size(
                            24.dp,
                        ).background(DSColor.White, CircleShape).border(1.dp, DSColor.Neutral200, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Box(Modifier.size(12.dp).background(DSColor.Black, CircleShape))
                    }
                    DSText(name, DSTextStyle.BodyL)
                }
            }
        }
    }
}
