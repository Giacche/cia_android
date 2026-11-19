package com.ueno.cia.feature.validatelimit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CPFInput
import com.ueno.cia.core.navigation.InputAmountInput
import com.ueno.cia.feature.validatelimit.data.ValidateLimitResponse
import com.ueno.cia.feature.validatelimit.domain.ValidateLimitUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class ValidateLimitState { Loading, Failed }

sealed interface ValidateLimitDestination {
    /** `pushReplacement`: esta pantalla no queda en el stack. */
    data class Replace(val route: CIARoute) : ValidateLimitDestination
    data object Back : ValidateLimitDestination
}

/**
 * Equivale a `ValidateLimitEntryPointNotifier` + `ValidateLimitNotifier`: pide la validación y decide la pantalla
 * siguiente según lo que traiga la respuesta. En Flutter eran dos pantallas encadenadas por `pushReplacement` solo
 * para inicializar el flow state; acá es una.
 */
class ValidateLimitViewModel(
    private val chargeType: ChargeType,
    private val provider: ChargeProvider,
    private val canPop: Boolean,
    private val useCase: ValidateLimitUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ValidateLimitState.Loading)
    val state: StateFlow<ValidateLimitState> = _state.asStateFlow()

    private val _destinations = Channel<ValidateLimitDestination>(Channel.BUFFERED)
    val destinations: Flow<ValidateLimitDestination> = _destinations.receiveAsFlow()

    private var loadJob: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.value = ValidateLimitState.Loading
        loadJob = viewModelScope.launch {
            try {
                route(useCase.validateLimit(chargeType, provider))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = ValidateLimitState.Failed
            }
        }
    }

    fun didTapBackFromError() {
        _destinations.trySend(ValidateLimitDestination.Back)
    }

    /** Prioridad de Flutter: tope transaccional → CPF → monto (con la pre-confirmación de link si vino). */
    private fun route(result: ValidateLimitResponse) {
        val route = when {
            result.transactionLimitScreen != null ->
                CIARoute.Congrats(CongratsInput(result.transactionLimitScreen, CongratsOrigin.TRANSACTION_LIMIT))
            result.cpfScreen != null ->
                CIARoute.Cpf(CPFInput(result.cpfScreen, result.amountScreen, chargeType, provider, canPop))
            else -> CIARoute.InputAmount(
                InputAmountInput(
                    screen = result.amountScreen,
                    chargeType = chargeType,
                    provider = provider,
                    linkPreConfirmScreen = result.linkPreConfirmScreen,
                    cpfNumber = null,
                    canPop = canPop,
                ),
            )
        }
        _destinations.trySend(ValidateLimitDestination.Replace(route))
    }
}
