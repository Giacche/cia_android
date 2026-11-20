package com.ueno.cia.feature.link.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.core.model.DetailCardItemModel
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DetailCard
import com.ueno.cia.designsystem.components.DetailRow
import com.ueno.cia.designsystem.components.FooterData
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.components.TermsCheckbox
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Punto de entrada de la pre-confirmación para el host. */
@Composable
fun LinkPreConfirmRoute(
    viewModel: LinkPreConfirmViewModel,
    onDestination: (LinkPreConfirmDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    LinkPreConfirmScreen(state, viewModel::didToggleAccepted, viewModel::didTapTyc, viewModel::didTapContinue, onBack, modifier)
}

/** Equivale a `LinkPreConfirmScreen`: "Revisá que todo esté bien", tarjeta de detalle y footer con T&C opcional y botón verde. */
@Composable
fun LinkPreConfirmScreen(
    state: LinkPreConfirmViewState,
    onToggleAccepted: () -> Unit,
    onTycClick: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screen = state.screen
    CIAScaffold(
        title = screen.appBarTitle,
        onBack = onBack,
        modifier = modifier.testTag(screen.tag ?: "cia_link_pre_confirm"),
        footer = {
            val tyc = state.tyc
            ScreenFooter(
                data = tyc?.let {
                    FooterData.Custom {
                        TermsCheckbox(
                            text = it.text,
                            linkText = it.linkText,
                            checked = state.accepted,
                            onToggle = onToggleAccepted,
                            onLinkClick = onTycClick,
                        )
                    }
                },
            ) {
                DSButton(
                    text = screen.primaryButton.text,
                    onClick = onContinue,
                    style = DSButtonStyle.Accent,
                    enabled = state.buttonEnabled,
                    modifier = Modifier.fillMaxWidth().testTag(screen.primaryButton.tag),
                )
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(
                rememberScrollState(),
            ).padding(horizontal = Spacing.l).padding(top = Spacing.xs, bottom = Spacing.l),
        ) {
            Spacer(Modifier.height(Spacing.xl))
            DSText(screen.title, DSTextStyle.DisplayS)
            Spacer(Modifier.height(Spacing.xl))
            DetailCard(screen.detailCard.title) {
                Column { screen.detailCard.items.orEmpty().forEach { ItemRow(it) } }
            }
        }
    }
}

/** Una fila con label vacío es un encabezado de sección (`overlineM`). */
@Composable
private fun ItemRow(item: DetailCardItemModel) {
    if (item.label.isEmpty()) {
        DSText(item.value, DSTextStyle.OverlineM, modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.xs))
    } else {
        DetailRow(label = item.label, value = item.value, badge = item.badge?.let { it.type to it.text })
    }
}
