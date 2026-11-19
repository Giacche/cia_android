package com.ueno.cia.feature.inputamount.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.QRAnalyticsNames
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.DetailCardItemModel
import com.ueno.cia.core.model.InputAmountScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CIARouteDestination
import com.ueno.cia.core.navigation.InputAmountInput
import com.ueno.cia.core.navigation.LinkFlowInput
import com.ueno.cia.core.navigation.QRFlowInput
import com.ueno.cia.core.util.CIACurrency
import com.ueno.cia.core.util.CurrencyFormatter
import com.ueno.cia.core.util.TemplateString
import java.math.BigDecimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Equivale a `InputAmountState` (input_amount_state.dart). */
data class InputAmountViewState(
    val screen: InputAmountScreenModel,
    val amount: BigDecimal = BigDecimal.ZERO,
    val commissionTitle: String,
    val commissionValue: String = "",
    val totalAmountTitle: String,
    val totalAmountValue: String = "",
    val errorMessage: String? = null,
    val showSummary: Boolean = false,
    val buttonDisabled: Boolean = true,
    val buttonText: String,
    val linkDescription: String? = "",
) {
    /** El resumen solo se ve si el BFF mandó los cuatro textos (Flutter: `showFooterSummary`). */
    val showFooterSummary: Boolean
        get() = showSummary && totalAmountTitle.isNotEmpty() && totalAmountValue.isNotEmpty() &&
            commissionTitle.isNotEmpty() && commissionValue.isNotEmpty()
}

sealed interface InputAmountDestination {
    data class Push(val route: CIARoute) : InputAmountDestination
    data object GoBackToHome : InputAmountDestination
    data object Back : InputAmountDestination
}

/** Equivale a `InputAmountNotifier` + la navegación de `InputAmountScreen._onButtonPressed`. */
class InputAmountViewModel(
    private val input: InputAmountInput,
    private val analytics: Analytics,
) : ViewModel() {
    private val formatter = CurrencyFormatter(CIACurrency.of(input.screen.data.currency))

    private val _state = MutableStateFlow(
        InputAmountViewState(
            screen = input.screen,
            commissionTitle = input.screen.summaryInfo.commissionTitle,
            totalAmountTitle = input.screen.summaryInfo.amountTitle,
            buttonText = input.screen.primaryButton.text,
        ),
    )
    val state: StateFlow<InputAmountViewState> = _state.asStateFlow()

    private val _destinations = Channel<InputAmountDestination>(Channel.BUFFERED)
    val destinations: Flow<InputAmountDestination> = _destinations.receiveAsFlow()

    val currency: CIACurrency get() = formatter.currency

    private val current: InputAmountViewState get() = _state.value

    // region Inputs

    /** `onChangeAmount`. */
    fun didChangeAmount(amount: BigDecimal) {
        validateAmountLimits(amount)
        update {
            it.copy(
                amount = amount,
                commissionValue = commissionDetail(amount),
                totalAmountValue = totalDetail(amount),
                showSummary = amount > BigDecimal.ZERO,
            )
        }
    }

    /** `updateLinkDescription`. */
    fun didChangeLinkDescription(description: String) {
        update { it.copy(linkDescription = description) }
        validateDescription(description)
        validateAmountLimits(current.amount)
    }

    fun didTapBack() {
        // Flutter: `canPop == null` (entrada desde la Home) → vuelve a la Home; si no, un pop al selector de origen
        _destinations.trySend(if (input.canPop) InputAmountDestination.Back else InputAmountDestination.GoBackToHome)
    }

    fun didTapContinue() {
        val state = current
        val label = state.buttonText
        val extra = mapOf(Analytics.SCREEN_NAME_KEY to QRAnalyticsNames.INPUT_AMOUNT_SCREEN_NAME)
        when (input.chargeType) {
            ChargeType.QR -> {
                analytics.flowContinue(
                    stepName = QRAnalyticsNames.INPUT_AMOUNT_STEP,
                    previousStepName = QRAnalyticsNames.startStep(input.provider),
                    label = label,
                    destination = CIARouteDestination.QR_GENERATE,
                    subFlow = QRAnalyticsNames.subFlow(input.provider),
                    extra = extra,
                )
                _destinations.trySend(InputAmountDestination.Push(CIARoute.QrGenerate(qrInput(state))))
            }
            ChargeType.LINK -> {
                val linkScreen = input.linkPreConfirmScreen ?: return
                val description = state.linkDescription.orEmpty()
                val amountFormatted = formatter.string(state.amount)
                val linkInput = LinkFlowInput(
                    chargeType = input.chargeType,
                    provider = input.provider,
                    amount = state.amount,
                    amountFormatted = amountFormatted,
                    description = description,
                    approximateToReceive = state.totalAmountValue,
                    linkPreConfirmScreen = linkScreen,
                    extraDetailItems = (if (description.isEmpty()) emptyList() else listOf(DetailCardItemModel("Descripción", description))) +
                        DetailCardItemModel("Total a cobrar", amountFormatted),
                )
                analytics.flowContinue(
                    stepName = QRAnalyticsNames.INPUT_AMOUNT_STEP,
                    previousStepName = "link_crear_inicio",
                    label = label,
                    destination = CIARouteDestination.LINK_PRE_CONFIRM,
                    subFlow = "link",
                    extra = extra,
                )
                _destinations.trySend(InputAmountDestination.Push(CIARoute.LinkPreConfirm(linkInput)))
            }
            // Tap to Phone usa su propia pantalla de monto (feature taptophone)
            ChargeType.TTP -> Unit
        }
    }

    // endregion

    // region Reglas (input_amount_notifier.dart)

    private fun qrInput(state: InputAmountViewState) =
        QRFlowInput(amount = state.amount, chargeType = input.chargeType, provider = input.provider, cpfNumber = input.cpfNumber)

    private fun update(transform: (InputAmountViewState) -> InputAmountViewState) = _state.update(transform)

    /** `_validateAmountLimits`: el máximo y el mínimo muestran error y bloquean el botón. */
    private fun validateAmountLimits(amount: BigDecimal) {
        val screen = current.screen
        val hasValidDescription = isDescriptionValid(current.linkDescription.orEmpty())
        when {
            amount > screen.maxAmount ->
                update { it.copy(errorMessage = screen.data.maxAmountErrorMessage, buttonDisabled = true) }
            amount > BigDecimal.ZERO && amount < screen.minAmount ->
                update { it.copy(errorMessage = screen.data.minAmountErrorMessage, buttonDisabled = true) }
            else ->
                update { it.copy(errorMessage = null, buttonDisabled = amount.signum() == 0 || !hasValidDescription) }
        }
    }

    /** `_validateDescription`. */
    private fun validateDescription(description: String) {
        if (current.screen.data.descriptionInput == null) return
        update { it.copy(buttonDisabled = description.isNotEmpty() && !isDescriptionValid(description)) }
    }

    /** `_isDescriptionValid`: largo mínimo y sin espacio final. */
    private fun isDescriptionValid(description: String): Boolean {
        val rule = current.screen.data.descriptionInput ?: return true
        return description.length >= rule.minLength && !description.endsWith(" ")
    }

    private fun commission(amount: BigDecimal): BigDecimal =
        amount.multiply(current.screen.commission).divide(BigDecimal(100))

    /** `_getComissionDetail`. */
    private fun commissionDetail(amount: BigDecimal): String =
        TemplateString.fill(
            current.screen.summaryInfo.commissionValueTemplate,
            mapOf(
                "commission" to dartDoubleString(current.screen.commission),
                "commission_amount" to formatter.string(commission(amount)),
            ),
        )

    /** `_getTotalDetail`: lo que recibe el comercio (monto menos comisión). */
    private fun totalDetail(amount: BigDecimal): String {
        val total = amount - commission(amount)
        return TemplateString.fill(current.screen.summaryInfo.amountValueTemplate, mapOf("amount" to formatter.string(total)))
    }

    // endregion

    companion object {
        /** Flutter imprime la comisión con `double.toString()`: `5` → `"5.0"`, `5.5` → `"5.5"`. */
        fun dartDoubleString(value: BigDecimal): String =
            if (value.compareTo(BigDecimal(value.toBigInteger())) == 0) "${value.toBigInteger()}.0"
            else value.stripTrailingZeros().toPlainString()
    }
}
