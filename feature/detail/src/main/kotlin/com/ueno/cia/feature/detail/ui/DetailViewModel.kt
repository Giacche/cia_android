package com.ueno.cia.feature.detail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.model.ReceiptScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CIARouteDestination
import com.ueno.cia.feature.detail.data.DetailScreenModel
import com.ueno.cia.feature.detail.domain.DetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Equivale a `DetailState`. */
data class DetailViewState(val screen: DetailScreenModel, val receipt: ReceiptScreenModel?)

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val value: DetailViewState) : DetailUiState
    data object Failed : DetailUiState
}

sealed interface DetailDestination {
    data class Push(val route: CIARoute) : DetailDestination
    data class Copy(val value: String, val message: String) : DetailDestination
}

/** Equivale a `DetailNotifier` + los callbacks de `DetailScreen`. */
class DetailViewModel(
    private val ticketNumber: String,
    private val channel: String?,
    private val useCase: DetailUseCase,
    private val analytics: Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private val _destinations = Channel<DetailDestination>(Channel.BUFFERED)
    val destinations: Flow<DetailDestination> = _destinations.receiveAsFlow()

    private var loadJob: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    /** `build` / `retry` (pull to refresh): vuelve a pedir el detalle mostrando el skeleton. */
    fun load() {
        loadJob?.cancel()
        _state.value = DetailUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = try {
                val response = useCase.getDetail(ticketNumber, channel)
                DetailUiState.Loaded(DetailViewState(response.detailScreen, response.receiptScreen))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                DetailUiState.Failed
            }
        }
    }

    /** `_onButtonPressed`: analytics + abre el comprobante. */
    fun didTapShareReceipt() {
        val state = (_state.value as? DetailUiState.Loaded)?.value ?: return
        val receipt = state.receipt ?: return
        analytics.buttonTap(
            label = state.screen.primaryButton?.text.orEmpty(),
            destination = CIARouteDestination.RECEIPT,
            subFlow = "detalle",
            extra = mapOf(Analytics.SCREEN_NAME_KEY to "cobros_detalle"),
        )
        _destinations.trySend(DetailDestination.Push(CIARoute.Receipt(receipt)))
    }

    /** `onActionLinkPressed`: solo `COPY` hace algo (copia el valor y avisa con un toast). */
    fun didTapActionLink(action: String?, value: String) {
        if (action != "COPY") return
        _destinations.trySend(DetailDestination.Copy(value, COPY_MESSAGE))
    }

    companion object {
        const val COPY_MESSAGE = "¡Copiaste el Nro de comprobante!"
    }
}
