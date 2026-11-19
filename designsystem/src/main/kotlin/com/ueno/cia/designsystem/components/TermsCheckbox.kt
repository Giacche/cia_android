package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

/** `TermsAndConditionsCheckbox`: casilla + texto con un link ("Acepto los {Términos y condiciones}"). */
@Composable
fun TermsCheckbox(
    text: String,
    linkText: String,
    checked: Boolean,
    onToggle: () -> Unit,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radius.x2s)
    val annotated = buildAnnotatedString {
        append("$text ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "tyc",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline, color = DSColor.Black)),
            ) { onLinkClick() },
        ) { append(linkText) }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (checked) DSColor.Black else DSColor.White, shape)
                .border(1.5.dp, DSColor.Black, shape)
                .semantics {
                    contentDescription = "$text $linkText"
                    stateDescription = if (checked) "1" else "0"
                }
                .clickable(role = Role.Checkbox, onClick = onToggle)
                .testTag("cia_tyc_checkbox"),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) DSIcon("check", tint = DSColor.White, size = 16.dp)
        }
        Text(
            text = annotated,
            style = DSTextStyle.BodyM.textStyle.copy(color = DSColor.Black),
            modifier = Modifier.weight(1f).clickable(onClick = onToggle),
        )
    }
}
