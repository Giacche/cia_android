package com.ueno.cia.feature.movements.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.EmptyStateModel
import com.ueno.cia.core.model.MovementActiveFilterModel
import com.ueno.cia.core.model.MovementChargeModel
import com.ueno.cia.core.model.MovementFiltersModel
import com.ueno.cia.core.model.MovementsFilterScreenModel
import com.ueno.cia.core.model.MovementsQuery
import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.core.model.MovementsScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.feature.movements.domain.MovementsUseCase
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

/** Equivale a `MovementsState`. */
data class MovementsViewState(
    val charges: List<MovementChargeModel>,
    val pageSize: Int,
    val tabs: List<MovementFiltersModel>,
    val currentTabIndex: Int,
    val selectedTabId: String,
    val page: Int = 1,
    val isLoadingMore: Boolean = false,
    val isFilterLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMoreMovements: Boolean,
    val emptyState: EmptyStateModel? = null,
    val filterButton: ButtonModel? = null,
    val activeFilters: List<MovementActiveFilterModel>? = null,
    val clearFiltersButton: ButtonModel? = null,
    val filterScreen: MovementsFilterScreenModel? = null,
    val isFilterActive: Boolean = false,
) {
    val hasActiveFilters: Boolean get() = !activeFilters.isNullOrEmpty()
}

sealed interface MovementsUiState {
    data object Loading : MovementsUiState
    data class Loaded(val value: MovementsViewState) : MovementsUiState
    data object Failed : MovementsUiState
}

sealed interface MovementsDestination {
    data class Push(val route: CIARoute) : MovementsDestination
}

/** Equivale a `MovementsNotifier`: historial paginado con pestañas, filtros aplicados y pull to refresh. */
class MovementsViewModel(
    private val input: MovementsScreenModel,
    private val useCase: MovementsUseCase,
    private val session: MovementsSession,
) : ViewModel() {
    private val _state = MutableStateFlow<MovementsUiState>(MovementsUiState.Loading)
    val state: StateFlow<MovementsUiState> = _state.asStateFlow()

    private val _destinations = Channel<MovementsDestination>(Channel.BUFFERED)
    val destinations: Flow<MovementsDestination> = _destinations.receiveAsFlow()

    val screen: MovementsScreenModel get() = input

    private var loadJob: Job? = null
    private var pageJob: Job? = null
    private var tabJob: Job? = null
    private var started = false

    private val loaded: MovementsViewState? get() = (_state.value as? MovementsUiState.Loaded)?.value

    init {
        // Al aplicar filtros se vuelve a cargar desde la primera página
        viewModelScope.launch { session.didApplyFilters.collect { load() } }
    }

    fun start() {
        if (started) return
        started = true
        load()
    }

    // region Carga (`build` / `_getMovements`)

    fun load() {
        pageJob?.cancel()
        tabJob?.cancel()
        loadJob?.cancel()
        _state.value = MovementsUiState.Loading
        loadJob = viewModelScope.launch {
            fetchFirstPage().fold(
                onSuccess = { _state.value = MovementsUiState.Loaded(it) },
                onFailure = { _state.value = MovementsUiState.Failed },
            )
        }
    }

    /** `chargesRetry` (pull to refresh): conserva la pantalla y recarga. */
    fun refresh() {
        if (loaded == null) return load()
        update { it.copy(isRefreshing = true) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            fetchFirstPage().fold(
                onSuccess = { _state.value = MovementsUiState.Loaded(it) },
                onFailure = { update { state -> state.copy(isRefreshing = false) } },
            )
        }
    }

    private suspend fun fetchFirstPage(): Result<MovementsViewState> {
        val tabId = session.selectedTabId
        val query = (session.appliedFilters ?: MovementsQuery(page = 1)).copy(page = 1, size = input.pageSize, type = tabId)
        return attempt { makeState(useCase.getMoves(query), tabId) }
    }

    private fun makeState(result: MovementsResponse, tabId: String): MovementsViewState {
        val tabs = resolveTabs(result, tabId)
        return MovementsViewState(
            charges = result.charges,
            pageSize = input.pageSize,
            tabs = tabs,
            currentTabIndex = tabs.indexOfFirst { it.active }.coerceAtLeast(0),
            selectedTabId = tabId,
            hasMoreMovements = result.charges.size >= input.pageSize,
            emptyState = result.emptyState,
            filterButton = result.filterButton,
            activeFilters = result.activeFilters,
            clearFiltersButton = result.clearFiltersButton,
            filterScreen = result.filterScreen,
            isFilterActive = session.appliedFilters != null,
        )
    }

    /** `_resolveTabs`: sin pestañas del BFF, una sola con el nombre según el tipo. */
    private fun resolveTabs(result: MovementsResponse, selectedTabId: String): List<MovementFiltersModel> {
        val serverTabs = result.tabsOptions
        if (serverTabs.isNullOrEmpty()) {
            return listOf(MovementFiltersModel(selectedTabId, if (selectedTabId == "services") "Servicios" else "Cobros", active = true))
        }
        return serverTabs.map { it.copy(active = it.id == selectedTabId) }
    }

    // endregion

    // region Paginación (`fetchNextPage`)

    fun loadNextPage() {
        val state = loaded ?: return
        if (!state.hasMoreMovements || state.isLoadingMore || state.isFilterLoading) return
        val nextPage = state.page + 1
        update { it.copy(page = nextPage, isLoadingMore = true) }
        val query = (session.appliedFilters ?: MovementsQuery(page = nextPage))
            .copy(page = nextPage, size = state.pageSize, type = state.selectedTabId)
        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            attempt { useCase.getMoves(query) }.fold(
                onSuccess = { result ->
                    update { current ->
                        if (result.charges.isEmpty()) {
                            current.copy(hasMoreMovements = false, isLoadingMore = false)
                        } else {
                            current.copy(
                                charges = current.charges + result.charges,
                                hasMoreMovements = result.charges.size >= current.pageSize,
                                isLoadingMore = false,
                            )
                        }
                    }
                },
                onFailure = { update { it.copy(hasMoreMovements = false, isLoadingMore = false) } },
            )
        }
    }

    // endregion

    // region Pestañas (`updateTabIndex`)

    fun didSelectTab(index: Int) {
        val state = loaded ?: return
        if (state.tabs.isEmpty()) return
        val valid = index.coerceIn(0, state.tabs.lastIndex)
        if (valid == state.currentTabIndex) return
        val selected = state.tabs[valid]
        if (selected.disabled) return

        session.selectedTabId = selected.id
        pageJob?.cancel()
        _state.value = MovementsUiState.Loaded(
            state.copy(
                currentTabIndex = valid,
                selectedTabId = selected.id,
                tabs = state.tabs.map { it.copy(active = it.id == selected.id) },
                charges = emptyList(),
                page = 1,
                isFilterLoading = true,
                activeFilters = emptyList(),
            ),
        )
        // Flutter consulta la pestaña nueva sin los filtros aplicados
        val query = MovementsQuery(page = 1, size = state.pageSize, type = selected.id)
        tabJob?.cancel()
        tabJob = viewModelScope.launch {
            attempt { useCase.getMoves(query) }.fold(
                onSuccess = { result ->
                    update {
                        it.copy(
                            charges = result.charges,
                            hasMoreMovements = result.charges.size >= it.pageSize,
                            emptyState = result.emptyState,
                            filterButton = result.filterButton,
                            activeFilters = result.activeFilters,
                            clearFiltersButton = result.clearFiltersButton,
                            filterScreen = result.filterScreen,
                            isFilterLoading = false,
                        )
                    }
                },
                onFailure = { update { it.copy(isFilterLoading = false) } },
            )
        }
    }

    // endregion

    // region Acciones

    /** `clearFilters`: quita los filtros aplicados y recarga. */
    fun clearFilters() {
        session.appliedFilters = null
        refresh()
    }

    fun didTapFilter() {
        val screen = loaded?.filterScreen ?: return
        _destinations.trySend(MovementsDestination.Push(CIARoute.MovementsFilters(screen)))
    }

    fun didTapCharge(charge: MovementChargeModel) {
        _destinations.trySend(
            MovementsDestination.Push(CIARoute.Detail(ticketNumber = charge.ticketNumber, channel = session.appliedFilters?.channel)),
        )
    }

    // endregion

    private fun update(transform: (MovementsViewState) -> MovementsViewState) {
        _state.update { current -> if (current is MovementsUiState.Loaded) MovementsUiState.Loaded(transform(current.value)) else current }
    }

    private companion object {
        /** Como `runCatching`, pero la cancelación de la corrutina se propaga. */
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
