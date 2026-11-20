package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

/**
 * `GuTextfield` (vía `TextFieldWrapper`): label arriba, caja con borde (azul con foco, rosa con error), y debajo la
 * ayuda o el error a la izquierda y el contador `n/max` a la derecha.
 */
@Composable
fun DSTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
    maxLength: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    requiredMark: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val hasError = !error.isNullOrEmpty()
    val message = if (hasError) error else helper
    val borderColor = when {
        hasError -> DSColor.Pink600
        focused -> DSColor.Blue500
        else -> DSColor.Neutral200
    }
    val shape = RoundedCornerShape(Radius.xs)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        DSText(if (requiredMark) "$label *" else label, DSTextStyle.BodyLStrong)
        BasicTextField(
            value = value,
            onValueChange = { new -> if (maxLength == null || new.length <= maxLength) onValueChange(new) },
            textStyle = DSTextStyle.BodyL.textStyle.copy(color = DSColor.Black),
            cursorBrush = SolidColor(DSColor.Blue500),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .background(DSColor.White, shape)
                        .border(1.dp, borderColor, shape)
                        .padding(horizontal = Spacing.m),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        DSText(placeholder, DSTextStyle.BodyL, color = DSColor.Neutral300)
                    }
                    inner()
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
            if (!message.isNullOrEmpty()) {
                DSText(
                    message,
                    DSTextStyle.BodyM,
                    color = if (hasError) DSColor.Pink600 else DSColor.Neutral500,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(Modifier.weight(1f))
            }
            if (maxLength != null) {
                DSText("${value.length}/$maxLength", DSTextStyle.BodyM, color = DSColor.Neutral500, textAlign = TextAlign.End)
            }
        }
    }
}
