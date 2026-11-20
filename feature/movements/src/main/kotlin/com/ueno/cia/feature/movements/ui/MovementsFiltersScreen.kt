package com.ueno.cia.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSSelectField
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DSTextField
import com.ueno.cia.designsystem.components.RadioOptionsSheet
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

private enum class FilterSheet { Channel, Method, Status }

/** Punto de entrada de los filtros para el host. */
@Composable
fun MovementsFiltersRoute(viewModel: MovementsFiltersViewModel, onPop: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pop by rememberUpdatedState(onPop)
    LaunchedEffect(viewModel) {
        viewModel.destinations.collect { destination ->
            when (destination) {
                MovementsFiltersDestination.Pop -> pop()
            }
        }
    }
    MovementsFiltersScreen(state, viewModel, onBack, modifier)
}

/**
 * Equivale a `MovementsFiltersScreen` + `MovementsFiltersWidget`: canal, método, estado, período y monto, con
 * "Limpiar filtros" / "Aplicar" abajo.
 */
@Composable
fun MovementsFiltersScreen(
    state: MovementsFiltersViewState,
    viewModel: MovementsFiltersViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screen = state.screen
    var sheet by remember { mutableStateOf<FilterSheet?>(null) }

    CIAScaffold(
        title = screen.appBarTitle,
        onBack = onBack,
        modifier = modifier.testTag(screen.tag),
        footer = {
            ScreenFooter(buttons = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    DSButton(
                        text = screen.secondaryButton.text,
                        onClick = viewModel::clearFilters,
                        style = DSButtonStyle.Secondary,
                        modifier = Modifier.weight(1f).testTag(screen.secondaryButton.tag),
                    )
                    DSButton(
                        text = screen.primaryButton.text,
                        onClick = viewModel::applyFilters,
                        style = DSButtonStyle.Primary,
                        modifier = Modifier.weight(1f).testTag(screen.primaryButton.tag),
                    )
                }
            })
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l),
        ) {
            if (state.hasChannelField) {
                DSSelectField(
                    label = screen.channel.label,
                    value = state.nameOf(state.selectedChannel, screen.channel.options),
                    hint = screen.channel.hint,
                    onClick = { sheet = FilterSheet.Channel },
                    requiredMark = true,
                    modifier = Modifier.testTag("cia_filter_channel"),
                )
                Spacer(Modifier.height(Spacing.l))
            }
            if (state.hasMethodField) {
                DSSelectField(
                    label = screen.method.label,
                    value = state.nameOf(state.selectedMethod, state.methodOptions),
                    hint = screen.method.hint,
                    onClick = { sheet = FilterSheet.Method },
                    enabled = state.methodEnabled,
                    modifier = Modifier.testTag("cia_filter_method"),
                )
                Spacer(Modifier.height(Spacing.l))
            }
            if (state.hasStatusField) {
                DSSelectField(
                    label = screen.status.label,
                    value = state.nameOf(state.selectedStatus, screen.status.options),
                    hint = screen.status.hint,
                    onClick = { sheet = FilterSheet.Status },
                    modifier = Modifier.testTag("cia_filter_status"),
                )
                Spacer(Modifier.height(Spacing.x2l))
            }

            DSText(screen.period.title, DSTextStyle.DisplayS)
            Spacer(Modifier.height(Spacing.m))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.Top) {
                DSTextField(
                    label = screen.period.from.title,
                    value = state.periodFrom.orEmpty(),
                    onValueChange = { viewModel.setPeriodFrom(MovementsFiltersViewModel.dateMask(it).ifEmpty { null }) },
                    placeholder = screen.period.from.hint,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f).testTag("cia_filter_period_from"),
                )
                DSTextField(
                    label = screen.period.to.title,
                    value = state.periodTo.orEmpty(),
                    onValueChange = { viewModel.setPeriodTo(MovementsFiltersViewModel.dateMask(it).ifEmpty { null }) },
                    placeholder = screen.period.to.hint,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f).testTag("cia_filter_period_to"),
                )
            }
            Spacer(Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                DSIcon("informationCircle", tint = DSColor.Neutral400, size = 14.dp, modifier = Modifier.size(14.dp))
                DSText(screen.period.description, DSTextStyle.BodyS, color = DSColor.Neutral400)
            }
            Spacer(Modifier.height(Spacing.x2l))

            if (state.hasAmountField) {
                DSText(screen.amount.title, DSTextStyle.DisplayS)
                Spacer(Modifier.height(Spacing.m))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.Top) {
                    DSTextField(
                        label = screen.amount.from.title,
                        value = state.amountFrom?.let { MovementsFiltersViewModel.thousands(it.toLong().toString()) }.orEmpty(),
                        onValueChange = {
                            viewModel.setAmountFrom(
                                MovementsFiltersViewModel.amountOf(MovementsFiltersViewModel.thousands(it)),
                            )
                        },
                        placeholder = screen.amount.from.hint,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f).testTag("cia_filter_amount_from"),
                    )
                    DSTextField(
                        label = screen.amount.to.title,
                        value = state.amountTo?.let { MovementsFiltersViewModel.thousands(it.toLong().toString()) }.orEmpty(),
                        onValueChange = {
                            viewModel.setAmountTo(
                                MovementsFiltersViewModel.amountOf(MovementsFiltersViewModel.thousands(it)),
                            )
                        },
                        placeholder = screen.amount.to.hint,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f).testTag("cia_filter_amount_to"),
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    when (sheet) {
        FilterSheet.Channel -> RadioOptionsSheet(
            title = screen.channel.label,
            options = screen.channel.options.map { it.id to it.name },
            selectedId = state.selectedChannel,
            onSelect = viewModel::selectChannel,
            onDismiss = { sheet = null },
        )
        FilterSheet.Method -> RadioOptionsSheet(
            title = screen.method.label,
            options = state.methodOptions.map { it.id to it.name },
            selectedId = state.selectedMethod,
            onSelect = viewModel::selectMethod,
            onDismiss = { sheet = null },
        )
        FilterSheet.Status -> RadioOptionsSheet(
            title = screen.status.label,
            options = screen.status.options.map { it.id to it.name },
            selectedId = state.selectedStatus,
            onSelect = viewModel::selectStatus,
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}
