package com.ueno.cia.feature.paymentselector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.model.HomeOptionKind
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.core.model.PaymentSelectorScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.PaymentSelectorInput
import com.ueno.cia.feature.paymentselector.domain.PaymentSelectorUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface PaymentSelectorDestination {
    data class Push(val route: CIARoute) : PaymentSelectorDestination

    /** `router.pushReplacement(PaymentSelectorScreen)`. */
    data class Replace(val route: CIARoute) : PaymentSelectorDestination
    data object Back : PaymentSelectorDestination
}

/**
 * Equivale a `PaymentSelectorEntryPointNotifier` + la `ref.listen` de `PaymentSelectorEntryPointScreen`:
 * pide las opciones de cobro y reemplaza esta pantalla por el selector.
 */
class PaymentSelectorEntryPointViewModel(
    private val type: String?,
    private val branchId: String?,
    private val origin: String?,
    private val useCase: PaymentSelectorUseCase,
) : ViewModel() {
    enum class State { Loading, Failed }

    private val _state = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _destinations = Channel<PaymentSelectorDestination>(Channel.BUFFERED)
    val destinations: Flow<PaymentSelectorDestination> = _destinations.receiveAsFlow()

    private var job: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    fun load() {
        job?.cancel()
        _state.value = State.Loading
        job = viewModelScope.launch {
            val response = try {
                useCase.entryPoint(type, branchId, origin)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = State.Failed
                return@launch
            }
            // Flutter: `Payment Selector screen not found`
            val screen = response.paymentEntryPoint ?: return@launch run { _state.value = State.Failed }
            _destinations.send(PaymentSelectorDestination.Replace(CIARoute.PaymentSelector(PaymentSelectorInput(screen, branchId, origin))))
        }
    }

    fun didTapBackFromError() {
        _destinations.trySend(PaymentSelectorDestination.Back)
    }
}

/** Equivale a `PaymentSelectorNotifier` + `PaymentSelectorScreen`: las opciones ya vienen cargadas por el entry point. */
class PaymentSelectorViewModel(
    private val input: PaymentSelectorInput,
    private val navigator: PaymentSelectorChargeNavigator,
) : ViewModel() {
    private val _destinations = Channel<PaymentSelectorDestination>(Channel.BUFFERED)
    val destinations: Flow<PaymentSelectorDestination> = _destinations.receiveAsFlow()

    val screen: PaymentSelectorScreenModel get() = input.screen

    fun didTapOption(option: HomeOptionModel) {
        if (option.disabled) return
        if (option.kind == HomeOptionKind.QrInternacional) {
            _destinations.trySend(
                PaymentSelectorDestination.Push(CIARoute.PaymentSelectorCountry(branchId = input.branchId, origin = INTERNATIONAL_ORIGIN)),
            )
            return
        }
        navigator.route(option)?.let { _destinations.trySend(PaymentSelectorDestination.Push(it)) }
    }

    fun didTapAlertAction() {
        val link = input.screen.data.informationAlert?.actionLink ?: return
        navigator.route(link)?.let { _destinations.trySend(PaymentSelectorDestination.Push(it)) }
    }

    companion object {
        /** `setXType('QR_INTERNATIONAL', xOrigin: 'SELECTOR_BUSINESS')` al elegir QR internacional. */
        const val INTERNATIONAL_ORIGIN = "SELECTOR_BUSINESS"
    }
}

sealed interface PaymentSelectorCountryState {
    data object Loading : PaymentSelectorCountryState
    data object Failed : PaymentSelectorCountryState
    data class Loaded(val screen: PaymentSelectorScreenModel) : PaymentSelectorCountryState
}

/**
 * Equivale a `PaymentSelectorCountryNotifier` + `PaymentSelectorCountryScreen`: pide al BFF las opciones de QR
 * internacional (una por país) y navega al cobro del país elegido.
 */
class PaymentSelectorCountryViewModel(
    private val branchId: String?,
    private val origin: String?,
    private val useCase: PaymentSelectorUseCase,
    private val navigator: PaymentSelectorChargeNavigator,
) : ViewModel() {
    private val _state = MutableStateFlow<PaymentSelectorCountryState>(PaymentSelectorCountryState.Loading)
    val state: StateFlow<PaymentSelectorCountryState> = _state.asStateFlow()

    private val _destinations = Channel<PaymentSelectorDestination>(Channel.BUFFERED)
    val destinations: Flow<PaymentSelectorDestination> = _destinations.receiveAsFlow()

    private var job: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    fun load() {
        job?.cancel()
        _state.value = PaymentSelectorCountryState.Loading
        job = viewModelScope.launch {
            _state.value = try {
                // Flutter: `Payment Selector country screen not found`
                useCase.entryPoint(X_TYPE, branchId, origin).paymentEntryPoint
                    ?.let { PaymentSelectorCountryState.Loaded(it) } ?: PaymentSelectorCountryState.Failed
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                PaymentSelectorCountryState.Failed
            }
        }
    }

    fun didTapOption(option: HomeOptionModel) {
        if (option.disabled) return
        navigator.route(option)?.let { _destinations.trySend(PaymentSelectorDestination.Push(it)) }
    }

    fun didTapAlertAction() {
        val screen = (_state.value as? PaymentSelectorCountryState.Loaded)?.screen ?: return
        val link = screen.data.informationAlert?.actionLink ?: return
        navigator.route(link)?.let { _destinations.trySend(PaymentSelectorDestination.Push(it)) }
    }

    fun didTapBackFromError() {
        _destinations.trySend(PaymentSelectorDestination.Back)
    }

    companion object {
        /** `_internationalQrXType`. */
        const val X_TYPE = "QR_INTERNATIONAL"
    }
}
