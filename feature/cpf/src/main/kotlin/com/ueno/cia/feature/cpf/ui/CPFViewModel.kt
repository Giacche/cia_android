package com.ueno.cia.feature.cpf.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.model.CpfScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CIARouteDestination
import com.ueno.cia.core.navigation.CPFInput
import com.ueno.cia.core.navigation.InputAmountInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class CPFViewState(
    val screen: CpfScreenModel,
    val cpfNumber: String = "",
    val error: String? = null,
    val helper: String? = null,
) {
    val buttonEnabled: Boolean get() = error == null && cpfNumber.isNotEmpty()
}

sealed interface CPFDestination {
    data class Push(val route: CIARoute) : CPFDestination
}

/** Equivale a `CpfNotifier` + `CpfScreen._onButtonPressed`. */
class CPFViewModel(
    private val input: CPFInput,
    private val analytics: Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow(CPFViewState(input.cpfScreen, helper = input.cpfScreen.numberInput.helper))
    val state: StateFlow<CPFViewState> = _state.asStateFlow()

    private val _destinations = Channel<CPFDestination>(Channel.BUFFERED)
    val destinations: Flow<CPFDestination> = _destinations.receiveAsFlow()

    /** `updateCpfNumber` + `CpfValidator.cpfNumber`. */
    fun didChangeCpf(value: String) {
        val number = value.trim()
        val model = input.cpfScreen.numberInput
        _state.update {
            val error = when {
                number.isEmpty() -> EMPTY_ERROR
                !Regex(model.regex).containsMatchIn(number) -> model.errorMessage
                else -> null
            }
            it.copy(cpfNumber = number, error = error, helper = if (error == null) model.helper else null)
        }
    }

    fun didTapContinue() {
        val state = _state.value
        if (!state.buttonEnabled) return
        analytics.flowContinue(
            stepName = "ingresar_cpf",
            previousStepName = "qr_pix_inicio",
            label = input.cpfScreen.primaryButton.text,
            destination = CIARouteDestination.INPUT_AMOUNT,
            subFlow = "qr_pix",
            extra = mapOf(Analytics.SCREEN_NAME_KEY to "cobros_ingresar_cpf"),
        )
        _destinations.trySend(
            CPFDestination.Push(
                CIARoute.InputAmount(
                    InputAmountInput(
                        screen = input.inputAmountScreen,
                        chargeType = input.chargeType,
                        provider = input.provider,
                        linkPreConfirmScreen = null,
                        cpfNumber = state.cpfNumber,
                        canPop = input.canPop,
                    ),
                ),
            ),
        )
    }

    private companion object {
        const val EMPTY_ERROR = "El CPF es obligatorio."
    }
}
