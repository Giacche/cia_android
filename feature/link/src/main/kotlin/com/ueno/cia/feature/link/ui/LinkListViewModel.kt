package com.ueno.cia.feature.link.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.feature.link.data.LinkListItemModel
import com.ueno.cia.feature.link.data.LinkListResponse
import com.ueno.cia.feature.link.data.LinkListScreenModel
import com.ueno.cia.feature.link.data.LinkListTabModel
import com.ueno.cia.feature.link.domain.LinkListUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Equivale a `LinkListState`. */
data class LinkListViewState(
    val screen: LinkListScreenModel,
    val currentTabIndex: Int = 0,
    val isLoadingItems: Boolean = false,
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingNextPage: Boolean = false,
) {
    val tabs: List<LinkListTabModel> get() = screen.tabsOptions
    val currentTab: LinkListTabModel? get() = tabs.getOrNull(currentTabIndex)
    val items: List<LinkListItemModel> get() = screen.tabItems.orEmpty()
}

sealed interface LinkListUiState {
    data object Loading : LinkListUiState
    data class Loaded(val value: LinkListViewState) : LinkListUiState
    data object Failed : LinkListUiState
}

sealed interface LinkListDestination {
    data class Push(val route: CIARoute) : LinkListDestination
    data object GoBackToHome : LinkListDestination
    data class Share(val text: String) : LinkListDestination
}

/** Equivale a `LinkListNotifier` (pestañas con carga por estado, paginación de 10 en 10 y cierre del mensaje informativo). */
class LinkListViewModel(
    private val useCase: LinkListUseCase,
    private val analytics: Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow<LinkListUiState>(LinkListUiState.Loading)
    val state: StateFlow<LinkListUiState> = _state.asStateFlow()

    private val _destinations = Channel<LinkListDestination>(Channel.BUFFERED)
    val destinations: Flow<LinkListDestination> = _destinations.receiveAsFlow()

    private var loadJob: Job? = null
    private var itemsJob: Job? = null
    private var pageJob: Job? = null
    private var started = false

    private val loaded: LinkListViewState? get() = (_state.value as? LinkListUiState.Loaded)?.value

    fun start() {
        if (started) return
        started = true
        load()
    }

    // region Carga inicial (`build`)

    fun load() {
        itemsJob?.cancel()
        pageJob?.cancel()
        loadJob?.cancel()
        _state.value = LinkListUiState.Loading
        loadJob = viewModelScope.launch {
            attempt { useCase.list("ACTIVE", page = 0, size = PAGE_SIZE) }.fold(
                onSuccess = ::handleLoaded,
                onFailure = { _state.value = LinkListUiState.Failed },
            )
        }
    }

    private fun handleLoaded(response: LinkListResponse) {
        val screen = response.linkListScreen ?: return run { _state.value = LinkListUiState.Failed }
        val selected = screen.tabsOptions.indexOfFirst { it.selected }.coerceAtLeast(0)
        analytics.screenView(subFlow = "link", screenName = "mis_links")
        _state.value = LinkListUiState.Loaded(
            LinkListViewState(screen, currentTabIndex = selected, isLastPage = screen.tabItems.orEmpty().size < PAGE_SIZE),
        )
    }

    // endregion

    // region Pestañas (`updateTabIndex` / `_fetchItemsForTab`)

    fun didSelectTab(index: Int) {
        val state = loaded ?: return
        if (state.tabs.isEmpty()) return
        val valid = index.coerceIn(0, state.tabs.lastIndex)
        if (valid == state.currentTabIndex) return
        _state.value = LinkListUiState.Loaded(state.copy(currentTabIndex = valid))
        val tab = state.tabs[valid]
        if (!tab.disabled) fetchItems(tab.id)
    }

    private fun fetchItems(status: String) {
        pageJob?.cancel()
        update { it.copy(isLoadingItems = true, isLoadingNextPage = false) }
        // Un cambio rápido de pestaña cancela la consulta anterior
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            attempt { useCase.list(status, page = 0, size = PAGE_SIZE) }.fold(
                onSuccess = { response ->
                    update { state ->
                        val new = response.linkListScreen
                        val items = new?.tabItems.orEmpty()
                        state.copy(
                            screen = state.screen.copy(
                                tabItems = items,
                                emptyState = new?.emptyState,
                                primaryButton = new?.primaryButton,
                                secondaryButton = new?.secondaryButton,
                            ),
                            isLoadingItems = false,
                            currentPage = 0,
                            isLastPage = items.size < PAGE_SIZE,
                        )
                    }
                },
                onFailure = { update { it.copy(isLoadingItems = false) } },
            )
        }
    }

    // endregion

    // region Paginación (`loadNextPage`)

    fun loadNextPage() {
        val state = loaded ?: return
        val tab = state.currentTab ?: return
        if (state.isLastPage || state.isLoadingNextPage || state.isLoadingItems) return
        val nextPage = state.currentPage + 1
        update { it.copy(isLoadingNextPage = true) }
        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            attempt { useCase.list(tab.id, page = nextPage, size = PAGE_SIZE) }.fold(
                onSuccess = { response ->
                    update { current ->
                        val new = response.linkListScreen?.tabItems.orEmpty()
                        current.copy(
                            screen = current.screen.copy(tabItems = current.items + new),
                            currentPage = nextPage,
                            isLastPage = new.size < PAGE_SIZE,
                            isLoadingNextPage = false,
                        )
                    }
                },
                onFailure = { update { it.copy(isLoadingNextPage = false) } },
            )
        }
    }

    // endregion

    // region Acciones

    /** `closeMessage`: quita el mensaje de la pestaña actual. */
    fun closeMessage() = update { state ->
        if (state.currentTabIndex !in state.screen.tabsOptions.indices) return@update state
        state.copy(
            screen = state.screen.copy(
                tabsOptions = state.screen.tabsOptions.mapIndexed { i, tab -> if (i == state.currentTabIndex) tab.copy(message = null) else tab },
            ),
        )
    }

    /** `onContinue`: abre el flujo de creación de link (validate → monto). */
    fun didTapContinue() {
        _destinations.trySend(LinkListDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.LINK, ChargeProvider.LINK)))
    }

    fun didTapGoBackHome() {
        _destinations.trySend(LinkListDestination.GoBackToHome)
    }

    /** `ActionHelper.execute(SHARE, content)`. */
    fun didTapShare(item: LinkListItemModel) {
        val content = item.actionButton?.content ?: return
        _destinations.trySend(LinkListDestination.Share(content))
    }

    // endregion

    private fun update(transform: (LinkListViewState) -> LinkListViewState) {
        _state.update { current -> if (current is LinkListUiState.Loaded) LinkListUiState.Loaded(transform(current.value)) else current }
    }

    companion object {
        const val PAGE_SIZE = 10

        suspend inline fun <T> attempt(block: () -> T): Result<T> =
            try {
                Result.success(block())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
    }
}
