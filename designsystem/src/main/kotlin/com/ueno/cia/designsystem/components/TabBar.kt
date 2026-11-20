package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

data class DSTab(val label: String, val disabled: Boolean = false, val icon: String? = null)

/**
 * `GuTabBar`: pestañas con texto en negrita, indicador negro debajo de la activa y divisor fino.
 * Soporta pestañas deshabilitadas y un ícono opcional a la izquierda del texto (`GuTab`).
 */
@Composable
fun DSTabBar(tabs: List<DSTab>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(56.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2s),
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val color = if (tab.disabled) DSColor.Neutral300 else DSColor.Black
                // `IntrinsicSize.Max`: el indicador ocupa el ancho de la pestaña aun dentro del scroll horizontal
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .semantics { this.selected = selected }
                        .clickable(enabled = !tab.disabled, role = Role.Tab) { if (!selected) onSelect(index) }
                        .testTag("cia_tab_$index"),
                ) {
                    Row(
                        modifier = Modifier.height(53.dp).padding(horizontal = Spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2s),
                    ) {
                        if (tab.icon != null) DSIcon(tab.icon, tint = color, size = 16.dp)
                        DSText(tab.label, DSTextStyle.BodyLStrong, color = color, maxLines = 1)
                    }
                    Box(
                        Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(
                                if (selected) DSColor.Black else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(topStart = 1.5.dp, topEnd = 1.5.dp),
                            ),
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Spacing.l)
                .fillMaxWidth()
                .height(1.dp)
                .background(DSColor.Neutral100),
        )
    }
}
