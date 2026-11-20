package com.ueno.cia.feature.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.ueno.cia.core.model.DetailCardItemModel
import com.ueno.cia.designsystem.components.AccordionCard
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSLinkButton
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DetailCard
import com.ueno.cia.designsystem.components.DetailRow
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.components.StepperList
import com.ueno.cia.designsystem.components.StepperStep
import com.ueno.cia.designsystem.components.stepperActiveIndex
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.images.hasDSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.detail.data.DetailPaymentTypeCardModel
import com.ueno.cia.feature.detail.data.DetailScreenModel
import com.ueno.cia.feature.detail.data.DetailStepperCardModel

/** Punto de entrada del Detalle para el host. */
@Composable
fun DetailRoute(
    viewModel: DetailViewModel,
    onDestination: (DetailDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    DetailScreen(
        state = state,
        onShareReceipt = viewModel::didTapShareReceipt,
        onActionLink = viewModel::didTapActionLink,
        onReload = viewModel::load,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Equivale a `DetailScreen` + `DetailScreenBody` + `DetailScreenFooter`: fecha, título e importe, estado del cobro
 * (stepper), medio de pago, detalle, descuentos (colapsable) y botón para compartir el comprobante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onShareReceipt: () -> Unit,
    onActionLink: (action: String?, value: String) -> Unit,
    onReload: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = (state as? DetailUiState.Loaded)?.value
    CIAScaffold(
        title = loaded?.screen?.appBarTitle.orEmpty(),
        onBack = onBack,
        modifier = modifier.testTag(loaded?.screen?.tag ?: "cia_detail"),
        footer = {
            val primary = loaded?.screen?.primaryButton
            if (primary != null) {
                ScreenFooter {
                    DSButton(
                        text = primary.text,
                        onClick = onShareReceipt,
                        style = DSButtonStyle.Primary,
                        modifier = Modifier.fillMaxWidth().testTag(primary.tag),
                    )
                }
            }
        },
    ) {
        when (state) {
            DetailUiState.Loading -> DetailSkeleton()
            DetailUiState.Failed -> FeedbackErrorScreen(onRetry = onReload, modifier = Modifier.testTag("cia_error_screen"))
            is DetailUiState.Loaded -> PullToRefreshBox(isRefreshing = false, onRefresh = onReload, modifier = Modifier.fillMaxSize()) {
                DetailBody(state.value.screen, onActionLink)
            }
        }
    }
}

@Composable
private fun DetailBody(screen: DetailScreenModel, onActionLink: (String?, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l).padding(top = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DSText(screen.date, DSTextStyle.BodyM, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.s))
        DSText(screen.title, DSTextStyle.DisplayS, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.s))
        DSText(screen.amount, DSTextStyle.DisplayL, textAlign = TextAlign.Center, modifier = Modifier.testTag("cia_detail_amount"))
        Spacer(Modifier.height(Spacing.xl))
        StepperCard(screen.stepperCard)
        Spacer(Modifier.height(Spacing.xl))
        PaymentCard(screen.paymentTypeCard)
        Spacer(Modifier.height(Spacing.xl))
        DetailCard(screen.detailCard.title) {
            Column { screen.detailCard.items.orEmpty().forEach { ItemRow(it, onActionLink) } }
        }
        screen.discountsCard?.let { discounts ->
            Spacer(Modifier.height(Spacing.xl))
            AccordionCard(title = discounts.title, expanded = false, headerModifier = Modifier.testTag("cia_detail_discounts")) {
                Column { discounts.items.orEmpty().forEach { ItemRow(it, onActionLink) } }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun StepperCard(card: DetailStepperCardModel) {
    AccordionCard(title = card.title, expanded = true, headerModifier = Modifier.testTag("cia_detail_stepper")) {
        if (card.items.size <= 1) {
            // Un solo paso: ícono de check verde con título (y descripción si hay)
            val item = card.items.firstOrNull()
            StepperList(listOf(StepperStep(item?.title.orEmpty(), item?.description, "completed")), activeIndex = 0)
        } else {
            val steps = card.items.map { StepperStep(it.title, it.description, it.status) }
            StepperList(steps, stepperActiveIndex(steps))
        }
    }
}

@Composable
private fun PaymentCard(card: DetailPaymentTypeCardModel) {
    DetailCard(card.title) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(DSColor.Blue100, CircleShape), contentAlignment = Alignment.Center) {
                DSIcon(if (hasDSIcon(card.icon)) card.icon else "charges", tint = DSColor.Blue800, size = 20.dp)
            }
            // `GuListItem.double(invert: true)`: el medio de pago arriba y el detalle gris abajo
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3s)) {
                DSText(card.subtitle, DSTextStyle.BodyL)
                if (!card.detail.isNullOrEmpty()) DSText(card.detail, DSTextStyle.BodyM, color = DSColor.Neutral500)
            }
        }
    }
}

/** Fila label gris / valor con link opcional a la derecha ("Copiar"). */
@Composable
private fun ItemRow(item: DetailCardItemModel, onActionLink: (String?, String) -> Unit) {
    if (item.label.isEmpty()) {
        DSText(item.value, DSTextStyle.OverlineM, modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.xs))
        return
    }
    DetailRow(
        label = item.label,
        value = item.value,
        trailing = item.actionLink?.let { link ->
            { DSLinkButton(link.text, onClick = { onActionLink(link.action, item.value) }, modifier = Modifier.testTag(link.tag)) }
        },
    )
}

@Composable
private fun DetailSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Skeleton(width = 160.dp, height = 14.dp)
        Skeleton(width = 140.dp, height = 24.dp)
        Skeleton(width = 200.dp, height = 32.dp)
        Skeleton(height = 180.dp, modifier = Modifier.padding(top = Spacing.xs))
        Skeleton(height = 110.dp)
        Skeleton(height = 200.dp)
    }
}
