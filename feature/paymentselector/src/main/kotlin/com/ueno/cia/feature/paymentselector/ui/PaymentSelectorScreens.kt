package com.ueno.cia.feature.paymentselector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.core.model.PaymentSelectorDataModel
import com.ueno.cia.core.model.gridAspectRatio
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.InlineMessage
import com.ueno.cia.designsystem.components.InlineMessageState
import com.ueno.cia.designsystem.components.QuickActionCard
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.components.TwoColumnGrid
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing

private const val APP_BAR_TITLE = "COBROS"

/**
 * Equivale a `PaymentSelectorOptionsView`: layout compartido por las pantallas tipo "selector de cobro" (método y
 * país). Solo dibuja título + alerta opcional + grilla; la navegación vive en el ViewModel.
 */
@Composable
private fun PaymentSelectorOptions(
    data: PaymentSelectorDataModel,
    onOptionTap: (HomeOptionModel) -> Unit,
    onAlertAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = Spacing.l, end = Spacing.l, top = Spacing.m, bottom = Spacing.xl),
    ) {
        Spacer(Modifier.height(Spacing.l))
        DSText(data.title, DSTextStyle.DisplayS, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        data.informationAlert?.let { alert ->
            Spacer(Modifier.height(Spacing.xl))
            InlineMessage(
                title = alert.title,
                description = alert.description,
                state = InlineMessageState.byName(alert.type),
                actionTitle = alert.actionLink?.text,
                onAction = onAlertAction,
                modifier = Modifier.testTag(alert.actionLink?.tag ?: "cia_selector_alert"),
            )
        }
        Spacer(Modifier.height(Spacing.x2l))
        TwoColumnGrid(
            aspectRatio = data.options.gridAspectRatio(),
            items = data.options.map { option ->
                { cell: Modifier ->
                    QuickActionCard(
                        title = option.title,
                        description = option.description.ifEmpty { null },
                        icon = option.icon,
                        disabled = option.disabled,
                        onClick = { onOptionTap(option) },
                        modifier = cell.testTag(if (option.tag.isEmpty()) option.id.lowercase() else "${option.tag}_${option.id.lowercase()}"),
                    )
                }
            },
        )
    }
}

/** `PaymentSelectorSkeletonWidget`: título + 2 × 2 cards. */
@Composable
private fun PaymentSelectorSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.l, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Skeleton(width = 220.dp, height = 20.dp) }
        Spacer(Modifier.height(Spacing.x2l - Spacing.m))
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                repeat(2) {
                    Column(
                        Modifier.weight(1f).height(130.dp).background(DSColor.White, RoundedCornerShape(Radius.s)).padding(Spacing.m),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Skeleton(width = 40.dp, height = 40.dp, circle = true)
                        Skeleton(width = 80.dp, height = 14.dp)
                    }
                }
            }
        }
    }
}

/** Equivale a `PaymentSelectorEntryPointScreen`: skeleton mientras se piden las opciones; si falla, error de pantalla completa. */
@Composable
fun PaymentSelectorEntryPointRoute(
    viewModel: PaymentSelectorEntryPointViewModel,
    onDestination: (PaymentSelectorDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    CIAScaffold(title = APP_BAR_TITLE, onBack = onBack, modifier = modifier.testTag("cia_payment_selector_entry_point")) {
        when (state) {
            PaymentSelectorEntryPointViewModel.State.Loading -> PaymentSelectorSkeleton()
            PaymentSelectorEntryPointViewModel.State.Failed ->
                FeedbackErrorScreen(onRetry = viewModel::didTapBackFromError, modifier = Modifier.testTag("cia_error_screen"))
        }
    }
}

/** Equivale a `PaymentSelectorScreen`: título, alerta y grilla de métodos de cobro del BFF. */
@Composable
fun PaymentSelectorRoute(
    viewModel: PaymentSelectorViewModel,
    onDestination: (PaymentSelectorDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    CIAScaffold(title = viewModel.screen.appBarTitle, onBack = onBack, modifier = modifier.testTag("cia_payment_selector")) {
        PaymentSelectorOptions(viewModel.screen.data, viewModel::didTapOption, viewModel::didTapAlertAction)
    }
}

/** Equivale a `PaymentSelectorCountryScreen`: opciones de QR internacional (una card por país, ícono = bandera). */
@Composable
fun PaymentSelectorCountryRoute(
    viewModel: PaymentSelectorCountryViewModel,
    onDestination: (PaymentSelectorDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    val loaded = state as? PaymentSelectorCountryState.Loaded
    CIAScaffold(title = loaded?.screen?.appBarTitle ?: APP_BAR_TITLE, onBack = onBack, modifier = modifier.testTag("cia_payment_selector_country")) {
        when (val current = state) {
            PaymentSelectorCountryState.Loading -> PaymentSelectorSkeleton()
            PaymentSelectorCountryState.Failed ->
                FeedbackErrorScreen(onRetry = viewModel::didTapBackFromError, modifier = Modifier.testTag("cia_error_screen"))
            is PaymentSelectorCountryState.Loaded -> PaymentSelectorOptions(current.screen.data, viewModel::didTapOption, viewModel::didTapAlertAction)
        }
    }
}
