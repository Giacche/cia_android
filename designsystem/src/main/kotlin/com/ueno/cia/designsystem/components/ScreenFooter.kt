package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Dato opcional que se muestra sobre los botones del footer. */
sealed interface FooterData {
    /** `GuFooterDataSingle`: label gris a la izquierda y valor en negrita a la derecha. */
    data class Single(val label: String, val value: String) : FooterData

    /** `GuFooterDataMultiple`: dos columnas (título en negrita y detalle gris debajo). */
    data class Multiple(
        val leftLabel: String,
        val leftValue: String,
        val rightLabel: String,
        val rightValue: String,
    ) : FooterData

    /** Vista propia sobre los botones (ej. casilla de términos y condiciones). */
    data class Custom(val content: @Composable () -> Unit) : FooterData
}

/** `GuFooter` con overlay: fondo blanco con sombra, dato opcional arriba y uno o dos botones apilados. */
@Composable
fun ScreenFooter(modifier: Modifier = Modifier, data: FooterData? = null, buttons: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dsShadow(DSShadow.L, RectangleShape)
            .background(DSColor.White)
            .navigationBarsPadding()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        when (data) {
            is FooterData.Single -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DSText(data.label, DSTextStyle.BodyL, color = DSColor.Neutral500, modifier = Modifier.weight(1f))
                DSText(data.value, DSTextStyle.BodyLStrong, textAlign = TextAlign.End)
            }
            is FooterData.Multiple -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
                    DSText(data.leftLabel, DSTextStyle.BodyLStrong)
                    DSText(data.leftValue, DSTextStyle.BodyM, color = DSColor.Neutral500)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2s), horizontalAlignment = Alignment.End) {
                    DSText(data.rightLabel, DSTextStyle.BodyLStrong, textAlign = TextAlign.End)
                    DSText(data.rightValue, DSTextStyle.BodyM, color = DSColor.Neutral500, textAlign = TextAlign.End)
                }
            }
            is FooterData.Custom -> data.content()
            null -> Unit
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), content = buttons)
    }
}
