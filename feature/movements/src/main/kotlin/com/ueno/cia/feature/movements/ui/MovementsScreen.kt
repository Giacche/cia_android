package com.ueno.cia.feature.movements.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSBadge
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSLinkButton
import com.ueno.cia.designsystem.components.DSTab
import com.ueno.cia.designsystem.components.DSTabBar
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.EmptyState
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.MovementRow
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Punto de entrada del historial para el host. */
@Composable
fun MovementsRoute(
    viewModel: MovementsViewModel,
    onDestination: (MovementsDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    MovementsScreen(viewModel, state, onBack, modifier)
}

/**
 * Equivale a `MovementsScreen` + `MovementsHistoryTabsWidget`: título con botón de filtros, pestañas, filtros activos,
 * lista paginada con pull to refresh y estado vacío.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(viewModel: MovementsViewModel, state: MovementsUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val screen = viewModel.screen
    CIAScaffold(
        title = if (state is MovementsUiState.Loaded) screen.appBarTitle else screen.appBarTitle,
        onBack = onBack,
        modifier = modifier.testTag(screen.tag),
    ) {
        when (state) {
            MovementsUiState.Loading -> MovementsSkeleton()
            MovementsUiState.Failed -> FeedbackErrorScreen(onRetry = viewModel::load, modifier = Modifier.testTag("cia_error_screen"))
            is MovementsUiState.Loaded -> {
                val loaded = state.value
                Column(Modifier.fillMaxSize()) {
                    Header(screen.title, loaded, viewModel::didTapFilter)
                    Spacer(Modifier.height(Spacing.m))
                    DSTabBar(
                        tabs = loaded.tabs.map { DSTab(it.title, it.disabled, it.icon.ifEmpty { null }) },
                        selectedIndex = loaded.currentTabIndex,
                        onSelect = viewModel::didSelectTab,
                    )
                    if (loaded.hasActiveFilters) ActiveFilters(loaded, viewModel::clearFilters)
                    Spacer(Modifier.height(Spacing.l))
                    PullToRefreshBox(
                        isRefreshing = loaded.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        Content(loaded, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, state: MovementsViewState, onFilter: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        DSText(title, DSTextStyle.DisplayS, maxLines = 1, modifier = Modifier.weight(1f))
        state.filterButton?.let {
            DSButton(it.text, onFilter, style = DSButtonStyle.SecondarySmall, modifier = Modifier.testTag("cia_movements_filter_btn"))
        }
    }
}

@Composable
private fun ActiveFilters(state: MovementsViewState, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.l, end = Spacing.l, top = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            state.activeFilters.orEmpty().forEach { DSBadge(type = "neutral", text = it.label) }
        }
        state.clearFiltersButton?.let {
            DSLinkButton(it.text, onClear, modifier = Modifier.testTag("cia_movements_clear_filters_btn"))
        }
    }
}

@Composable
private fun Content(state: MovementsViewState, viewModel: MovementsViewModel) {
    val showEmpty = !state.isFilterLoading && state.charges.isEmpty()
    when {
        state.isFilterLoading -> Box(Modifier.fillMaxSize().padding(top = Spacing.x3l), contentAlignment = Alignment.TopCenter) {
            CircularProgressIndicator(color = DSColor.Black, strokeWidth = 3.dp)
        }
        showEmpty -> Box(Modifier.fillMaxSize().padding(horizontal = Spacing.l), contentAlignment = Alignment.Center) {
            state.emptyState?.let { EmptyState(it.illustration, it.title, it.subtitle) }
        }
        else -> ChargesList(state, viewModel)
    }
}

@Composable
private fun ChargesList(state: MovementsViewState, viewModel: MovementsViewModel) {
    val listState = rememberLazyListState()
    val loadMore by rememberUpdatedState(viewModel::loadNextPage)
    // Pide la página siguiente al acercarse al final, o si la lista no llena la pantalla (`_checkIfContentFitsScreen`)
    LaunchedEffect(listState, state.charges.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.canScrollForward }
            .collect { (last, canScroll) ->
                if (last != null && (last >= state.charges.size - 2 || !canScroll)) loadMore()
            }
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("cia_movements_list")) {
        itemsIndexed(state.charges, key = { index, charge -> "${charge.ticketNumber}_$index" }) { _, charge ->
            MovementRow(
                title = charge.title,
                subtitle = charge.subtitle,
                amount = charge.amount,
                date = charge.date,
                onClick = { viewModel.didTapCharge(charge) },
                modifier = Modifier.padding(horizontal = Spacing.l).testTag("cia_movement_${charge.ticketNumber}"),
            )
        }
        item {
            if (state.isLoadingMore) {
                Box(Modifier.padding(horizontal = Spacing.l)) { RowSkeleton() }
            } else {
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun RowSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Skeleton(width = 124.dp, height = 16.dp)
            Skeleton(width = 92.dp, height = 14.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = Alignment.End) {
            Skeleton(width = 102.dp, height = 16.dp)
            Skeleton(width = 22.dp, height = 14.dp)
        }
    }
}

@Composable
private fun MovementsSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(start = Spacing.l, end = Spacing.l, top = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Skeleton(width = 100.dp, height = 24.dp)
            Skeleton(width = 84.dp, height = 32.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Skeleton(width = 72.dp, height = 32.dp)
            Skeleton(width = 96.dp, height = 32.dp)
        }
        Column { repeat(4) { RowSkeleton() } }
    }
}
