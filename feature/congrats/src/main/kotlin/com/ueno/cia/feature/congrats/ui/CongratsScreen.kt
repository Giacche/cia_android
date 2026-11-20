package com.ueno.cia.feature.congrats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.model.CongratsCardItemModel
import com.ueno.cia.core.model.CongratsCardModel
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DetailCard
import com.ueno.cia.designsystem.components.DetailRow
import com.ueno.cia.designsystem.components.StatusBarIcons
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.images.DSIllustration
import com.ueno.cia.designsystem.images.hasDSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.designsystem.R as DSR

/** Punto de entrada del resultado para el host: conecta el [CongratsViewModel] con la pantalla. */
@Composable
fun CongratsRoute(viewModel: CongratsViewModel, onDestination: (CongratsDestination) -> Unit, modifier: Modifier = Modifier) {
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    // Sin back: Flutter usa `PopScope(canPop: false)`
    BackHandler {}
    CongratsScreen(
        viewModel = viewModel,
        modifier = modifier,
    )
}

/** Equivale a `CongratsScreen` + `GuFeedbackPage`: ícono o ilustración, título, descripción, tarjeta y botones. */
@Composable
fun CongratsScreen(viewModel: CongratsViewModel, modifier: Modifier = Modifier) {
    StatusBarIcons(darkIcons = true)
    val screen = viewModel.screen
    Box(modifier.fillMaxSize().background(DSColor.Neutral50).testTag(screen.tag)) {
        if (viewModel.kind == CongratsKind.Semantic) SemanticLayout(viewModel) else CenteredLayout(viewModel)
    }
}

// region Semántico (`SemanticFeedbackPage`)

private data class SemanticStyle(val color: Color, val illustration: String)

private fun semanticStyle(status: String): SemanticStyle = when (status) {
    "success" -> SemanticStyle(DSColor.Green300, "semanticSuccess")
    "warning" -> SemanticStyle(DSColor.Yellow300, "semanticWarning")
    "info", "information" -> SemanticStyle(DSColor.Blue300, "semanticInformation")
    else -> SemanticStyle(DSColor.Pink300, "semanticError")
}

/**
 * Encabezado de color con la ilustración del estado y, debajo, una hoja de esquinas redondeadas con título,
 * descripción, tarjeta de detalle y botones.
 */
@Composable
private fun SemanticLayout(viewModel: CongratsViewModel) {
    val data = viewModel.screen.data
    val style = semanticStyle(data.semanticStatus)
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().background(style.color).statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.fillMaxWidth().height(80.dp + Spacing.xl * 2 + Radius.xl), contentAlignment = Alignment.TopCenter) {
                DSIllustration(
                    name = style.illustration,
                    modifier = Modifier
                        .padding(top = Spacing.xl)
                        .size(80.dp)
                        .semantics { contentDescription = data.semanticStatus }
                        .testTag("cia_congrats_status_${data.semanticStatus}"),
                )
            }
        }
        // La hoja sube sobre el color para redondear las esquinas
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 0.dp)
                .background(style.color),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DSColor.Neutral50, RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl)),
            ) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l).padding(top = Spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(Spacing.s))
                    DSText(data.title, DSTextStyle.DisplayS, textAlign = TextAlign.Center)
                    if (!data.description.isNullOrEmpty()) {
                        DSText(data.description!!, DSTextStyle.BodyL, textAlign = TextAlign.Center)
                    }
                    data.card?.let { card ->
                        Spacer(Modifier.height(Spacing.x2l))
                        CongratsCard(card, viewModel::didTapCardAction)
                    }
                    Spacer(Modifier.height(Spacing.l))
                }
                Buttons(viewModel, Modifier.padding(horizontal = Spacing.l).padding(top = Spacing.s))
            }
        }
    }
}

@Composable
private fun CongratsCard(card: CongratsCardModel, onAction: (CongratsCardItemModel) -> Unit) {
    DetailCard(card.title) {
        card.items.forEach { item ->
            DetailRow(
                label = item.label,
                value = item.value,
                trailing = item.actionButton?.let { action ->
                    {
                        // Ícono del BFF o, si no existe en el design system, uno equivalente
                        val icon = if (hasDSIcon(action.icon)) action.icon else if (action.action == "COPY") "duplicate" else "share"
                        Box(
                            Modifier
                                .size(40.dp)
                                .semantics { contentDescription = action.action }
                                .clickable(role = Role.Button) { onAction(item) }
                                .testTag(action.tag),
                            contentAlignment = Alignment.Center,
                        ) { DSIcon(icon, tint = DSColor.Black, size = 24.dp) }
                    }
                },
            )
        }
    }
}

// endregion

// region Neutral y error

@Composable
private fun CenteredLayout(viewModel: CongratsViewModel) {
    val data = viewModel.screen.data
    val isError = viewModel.kind == CongratsKind.Error
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl)
                    .padding(bottom = Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val illustration = if (isError) "errorDialog" else data.illustration
                if (!illustration.isNullOrEmpty()) {
                    DSIllustration(illustration, modifier = Modifier.height(120.dp))
                    Spacer(Modifier.height(Spacing.s))
                }
                DSText(
                    text = if (isError) stringResource(DSR.string.ds_error_screen_title) else data.title,
                    style = DSTextStyle.DisplayM,
                    textAlign = TextAlign.Center,
                )
                val description = if (isError) stringResource(DSR.string.ds_error_screen_description) else data.description
                if (!description.isNullOrEmpty()) {
                    DSText(description, DSTextStyle.BodyL, color = DSColor.Neutral600, textAlign = TextAlign.Center)
                }
            }
        }
        Buttons(viewModel, Modifier.padding(horizontal = Spacing.l))
    }
}

// endregion

@Composable
private fun Buttons(viewModel: CongratsViewModel, modifier: Modifier = Modifier) {
    val screen = viewModel.screen
    val isError = viewModel.kind == CongratsKind.Error
    Column(
        modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        DSButton(
            text = if (isError) stringResource(DSR.string.ds_retry) else screen.primaryButton.text,
            onClick = viewModel::didTapPrimary,
            style = DSButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth().testTag(if (isError) "cia_error_retry_btn" else screen.primaryButton.tag),
        )
        val secondary = screen.secondaryButton
        if (!isError && secondary != null && secondary.text.isNotEmpty()) {
            DSButton(
                text = secondary.text,
                onClick = viewModel::didTapSecondary,
                style = DSButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth().testTag(secondary.tag),
            )
        }
    }
}
