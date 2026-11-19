package com.ueno.cia.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ueno.cia.core.util.CIACurrency
import com.ueno.cia.core.util.CurrencyFormatter
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * `GuAmount` (sin slider): símbolo de moneda + monto grande centrado, con separador de miles en vivo, teclado
 * numérico, sacudida al pasar el máximo de dígitos y mensaje de error debajo (rosa, con ícono).
 *
 * [onAmountChange] se llama solo por cambios del usuario (vacío = 0), igual que `onChanged` en Flutter.
 */
@Composable
fun AmountInput(
    currency: CIACurrency,
    onAmountChange: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    maxDigits: Int = 15,
    autoFocus: Boolean = true,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    val formatted = CurrencyFormatter.group(digits, currency.thousandsSeparator).takeIf { digits.isNotEmpty() }.orEmpty()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // El monto se achica para entrar en el ancho (iOS: adjustsFontSizeToFitWidth, mínimo 14)
            val available = maxWidth - 64.dp
            val chars = maxOf(formatted.length, 1)
            val size = (available.value / (chars * 0.66f)).coerceIn(14f, 32f)
            val textStyle = DSTextStyle.DisplayXL.textStyle.copy(
                fontSize = size.sp,
                lineHeight = (size * 1.25f).sp,
                color = DSColor.Black,
            )
            Row(
                modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset((sin(shake.value * 2 * PI * 2.5) * 10 * (1 - shake.value) * density).roundToInt(), 0) },
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2s),
                verticalAlignment = Alignment.Bottom,
            ) {
                DSText(currency.symbol, DSTextStyle.DisplayS, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
                BasicTextField(
                    value = TextFieldValue(formatted, selection = TextRange(formatted.length)),
                    onValueChange = { new ->
                        val entered = new.text.filter { it.isDigit() }
                        if (entered.length > maxDigits) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                shake.snapTo(0f)
                                shake.animateTo(1f, tween(350, easing = LinearEasing))
                            }
                        } else {
                            // Sin ceros a la izquierda ("007" → "7")
                            digits = entered.trimStart('0')
                            onAmountChange(digits.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                        }
                    },
                    textStyle = textStyle,
                    cursorBrush = SolidColor(DSColor.Blue500),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = currency.symbol }
                        .testTag("cia_amount_input"),
                    decorationBox = { inner ->
                        androidx.compose.foundation.layout.Box {
                            // El hint ("0") se ve igual que el texto, como en Flutter
                            if (digits.isEmpty()) androidx.compose.material3.Text("0", style = textStyle)
                            inner()
                        }
                    },
                )
            }
        }
        if (!errorMessage.isNullOrEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2s),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("cia_amount_error"),
            ) {
                DSIcon("warning", tint = DSColor.Pink600, size = 12.dp)
                DSText(errorMessage, DSTextStyle.BodyM, color = DSColor.Pink600, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}
