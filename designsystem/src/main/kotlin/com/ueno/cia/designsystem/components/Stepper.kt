package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

data class StepperStep(
    val title: String,
    val description: String?,
    /** `completed`, `info`, `error`, `warning`; cualquier otro valor es deshabilitado. */
    val status: String,
)

/** Primer paso `info`; si no hay, el último `completed`; si no, el primero. */
fun stepperActiveIndex(steps: List<StepperStep>): Int {
    steps.indexOfFirst { it.status == "info" }.takeIf { it >= 0 }?.let { return it }
    return steps.indexOfLast { it.status == "completed" }.takeIf { it >= 0 } ?: 0
}

/**
 * `GuListStepper`: pasos con ícono de estado, línea vertical que los une y texto según el paso activo
 * (anteriores en gris, el activo en negro, posteriores en gris claro).
 */
@Composable
fun StepperList(steps: List<StepperStep>, activeIndex: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        steps.forEachIndexed { index, step ->
            StepRow(step, index, activeIndex, isLast = index == steps.lastIndex)
        }
    }
}

@Composable
private fun StepRow(step: StepperStep, index: Int, activeIndex: Int, isLast: Boolean) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(step.title, step.description).joinToString(", ")
            },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            StepIcon(step.status)
            if (!isLast) {
                // La línea del paso activo es clara; antes del activo, oscura; después, clara
                Box(
                    Modifier
                        .width(1.dp)
                        .weight(1f)
                        .background(if (index >= activeIndex) DSColor.Neutral300 else DSColor.Neutral500),
                )
            }
        }
        Column(Modifier.padding(bottom = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
            DSText(
                step.title,
                DSTextStyle.BodyLStrong,
                color = when {
                    index < activeIndex -> DSColor.Neutral500
                    index == activeIndex -> DSColor.Black
                    else -> DSColor.Neutral300
                },
            )
            if (!step.description.isNullOrEmpty()) DSText(step.description, DSTextStyle.BodyM, color = DSColor.Neutral500)
        }
    }
}

@Composable
private fun StepIcon(status: String) {
    val (color, glyph) = when (status) {
        "completed" -> DSColor.Green500 to "check"
        "info" -> DSColor.Blue500 to "information"
        "error" -> DSColor.Pink500 to "warning"
        "warning" -> DSColor.Yellow500 to "warning"
        else -> DSColor.Neutral300 to null
    }
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (glyph != null) {
            Box(Modifier.size(24.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                DSIcon(glyph, tint = Color.White, size = 16.dp)
            }
        } else {
            // Deshabilitado: punto hueco
            Box(Modifier.size(12.dp).background(DSColor.White, CircleShape).border(1.dp, DSColor.Neutral300, CircleShape))
        }
    }
}
