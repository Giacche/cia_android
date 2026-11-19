package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.designsystem.R as DSR
import com.ueno.cia.designsystem.components.DSIconButton
import com.ueno.cia.designsystem.components.DSIconButtonMode
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** App bar de la Home sobre el header oscuro: atrás, título y accesos a perfil / ayuda. */
@Composable
fun HomeTopBar(
    title: String,
    isEmpresas: Boolean,
    profile: ButtonModel?,
    help: ButtonModel?,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flutter: modo light en Empresas (header violeta), dark en Individuos
    val mode = if (isEmpresas) DSIconButtonMode.Light else DSIconButtonMode.Dark
    Box(modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = Spacing.l)) {
        DSIconButton(
            icon = "arrowLeft",
            mode = mode,
            contentDescription = stringResource(DSR.string.ds_back),
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).testTag("cia_home_back_btn"),
        )
        DSText(
            text = title.uppercase(),
            style = DSTextStyle.OverlineM,
            color = DSColor.Neutral500,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center).semantics { heading() },
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            // Los botones solo aparecen si el BFF manda `icon` (igual que Flutter)
            if (profile?.icon != null) {
                DSIconButton(profile.icon!!, mode, profile.text, onProfile, Modifier.testTag(profile.tag))
            }
            if (help?.icon != null) {
                DSIconButton(help.icon!!, mode, help.text, onHelp, Modifier.testTag(help.tag))
            }
        }
    }
}
