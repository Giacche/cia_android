package com.ueno.cia.feature.link.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSTab
import com.ueno.cia.designsystem.components.DSTabBar
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.InlineMessage
import com.ueno.cia.designsystem.components.InlineMessageState
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.images.DSIllustration
import com.ueno.cia.designsystem.images.hasDSIcon
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.link.data.LinkListItemModel

/** Punto de entrada de "Mis links" para el host. */
@Composable
fun LinkListRoute(
    viewModel: LinkListViewModel,
    onDestination: (LinkListDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    LinkListScreen(viewModel, state, onBack, modifier)
}

/**
 * Equivale a `LinkListScreen` + `LinkListTabContentWidget`: pestañas por estado, lista paginada con mensaje
 * informativo, skeleton al cambiar de pestaña y estado vacío con botones.
 */
@Composable
fun LinkListScreen(
    viewModel: LinkListViewModel,
    state: LinkListUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = (state as? LinkListUiState.Loaded)?.value
    val showEmpty = loaded != null && !loaded.isLoadingItems && loaded.items.isEmpty()
    CIAScaffold(
        title = loaded?.screen?.appBarTitle.orEmpty(),
        onBack = onBack,
        modifier = modifier.testTag("cia_payment_link_list_screen"),
        footer = {
            if (showEmpty && loaded != null) {
                val screen = loaded.screen
                ScreenFooter {
                    // Con botón principal: doble (principal + secundario). Sin él: un solo botón con el texto del secundario.
                    val primary = screen.primaryButton
                    if (primary != null) {
                        DSButton(primary.text, viewModel::didTapContinue, style = DSButtonStyle.Primary, modifier = Modifier.fillMaxWidth().testTag(primary.tag))
                        DSButton(
                            screen.secondaryButton?.text.orEmpty(),
                            viewModel::didTapGoBackHome,
                            style = DSButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth().testTag(screen.secondaryButton?.tag ?: "cia_link_home"),
                        )
                    } else {
                        DSButton(
                            screen.secondaryButton?.text.orEmpty(),
                            viewModel::didTapGoBackHome,
                            style = DSButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth().testTag(screen.secondaryButton?.tag ?: "cia_link_home"),
                        )
                    }
                }
            }
        },
    ) {
        when (state) {
            LinkListUiState.Loading -> ListSkeleton()
            LinkListUiState.Failed -> FeedbackErrorScreen(onRetry = viewModel::load, modifier = Modifier.testTag("cia_error_screen"))
            is LinkListUiState.Loaded -> Column(Modifier.fillMaxSize()) {
                val value = state.value
                DSTabBar(
                    tabs = value.tabs.map { DSTab(it.label, it.disabled) },
                    selectedIndex = value.currentTabIndex,
                    onSelect = viewModel::didSelectTab,
                )
                when {
                    value.isLoadingItems -> Column(Modifier.padding(horizontal = Spacing.l).padding(top = Spacing.xs)) { repeat(3) { RowSkeleton() } }
                    showEmpty -> EmptyContent(value)
                    else -> LinksList(value, viewModel)
                }
            }
        }
    }
}

@Composable
private fun LinksList(state: LinkListViewState, viewModel: LinkListViewModel) {
    val listState = rememberLazyListState()
    val loadMore by rememberUpdatedState(viewModel::loadNextPage)
    // `_checkIfNeedsMoreItems` + último elemento visible: pide la página siguiente
    LaunchedEffect(listState, state.items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.canScrollForward }
            .collect { (last, canScroll) ->
                if (last != null && (last >= state.items.size - 1 || !canScroll)) loadMore()
            }
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("cia_link_list")) {
        state.currentTab?.message?.let { message ->
            item(key = "message") {
                Box(Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.m + Spacing.xs, bottom = Spacing.m)) {
                    InlineMessage(
                        title = null,
                        description = message.message,
                        state = InlineMessageState.byName(message.type),
                        onClose = viewModel::closeMessage,
                    )
                }
            }
        }
        itemsIndexed(state.items, key = { index, _ -> index }) { index, item ->
            LinkRow(item, onShare = { viewModel.didTapShare(item) }, modifier = Modifier.testTag("cia_link_row_$index"))
        }
        item(key = "footer") {
            if (state.isLoadingNextPage) Box(Modifier.padding(horizontal = Spacing.l)) { RowSkeleton() } else Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** `GuListItem.double(invert: true)`: título arriba, valor gris abajo y botón de acción (compartir) a la derecha. */
@Composable
private fun LinkRow(item: LinkListItemModel, onShare: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = Spacing.l, end = Spacing.l - Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
            DSText(item.label, DSTextStyle.BodyL)
            DSText(item.value, DSTextStyle.BodyM, color = DSColor.Neutral500)
        }
        item.actionButton?.let { action ->
            Box(
                Modifier
                    .size(44.dp)
                    .semantics { contentDescription = if (action.action == "SHARE") "Compartir" else action.action }
                    .clickable(role = Role.Button, onClick = onShare)
                    .testTag(action.tag),
                contentAlignment = Alignment.Center,
            ) { DSIcon(if (hasDSIcon(action.icon)) action.icon else "share", tint = DSColor.Black, size = 24.dp) }
        }
    }
}

@Composable
private fun EmptyContent(state: LinkListViewState) {
    val empty = state.screen.emptyState
    Box(Modifier.fillMaxSize().padding(horizontal = Spacing.l), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
            DSIllustration(empty?.illustration?.takeIf { it.isNotEmpty() } ?: "searchIllustration", modifier = Modifier.height(160.dp))
            Spacer(Modifier.height(Spacing.xs))
            DSText(empty?.title.orEmpty(), DSTextStyle.DisplayM, textAlign = TextAlign.Center)
            DSText(empty?.subtitle.orEmpty(), DSTextStyle.BodyL, textAlign = TextAlign.Center)
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
            Skeleton(width = 140.dp, height = 16.dp)
            Skeleton(width = 90.dp, height = 14.dp)
        }
        Skeleton(width = 24.dp, height = 24.dp, circle = true)
    }
}

@Composable
private fun ListSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = Spacing.xl, end = Spacing.l, top = Spacing.m, bottom = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Skeleton(width = 72.dp, height = 20.dp)
            Skeleton(width = 72.dp, height = 20.dp)
            Skeleton(width = 80.dp, height = 20.dp)
        }
        Column(Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.m)) { repeat(5) { RowSkeleton() } }
    }
}
