package com.ueno.cia.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

/** `GuCardAccordion`: tarjeta con título y chevron que despliega / pliega su contenido. */
@Composable
fun AccordionCard(
    title: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isExpanded by rememberSaveable(title) { mutableStateOf(expanded) }
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "accordion_chevron")
    val shape = RoundedCornerShape(Radius.xs)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dsShadow(DSShadow.M, shape)
            .background(DSColor.White, shape)
            .animateContentSize(),
    ) {
        Row(
            modifier = headerModifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics { stateDescription = if (isExpanded) "Expandido" else "Contraído" }
                .clickable(role = Role.Button) { isExpanded = !isExpanded }
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DSText(title, DSTextStyle.BodyLStrong, maxLines = 1, modifier = Modifier.weight(1f))
            DSIcon("chevronDown", tint = DSColor.Black, size = 24.dp, modifier = Modifier.rotate(rotation))
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                Box(Modifier.padding(horizontal = Spacing.l).fillMaxWidth().height(1.dp).background(DSColor.Neutral100))
                Box(Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.l)) { content() }
            }
        }
    }
}
