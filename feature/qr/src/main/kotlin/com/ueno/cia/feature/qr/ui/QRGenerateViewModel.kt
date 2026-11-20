package com.ueno.cia.feature.qr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.QRAnalyticsNames
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.model.QRTimeoutModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.QRFlowInput
import com.ueno.cia.core.network.APIError
import com.ueno.cia.core.platform.ConnectivityChecking
import com.ueno.cia.core.util.CIACurrency
import com.ueno.cia.core.util.CurrencyFormatter
import com.ueno.cia.core.util.TemplateString
import com.ueno.cia.feature.qr.data.QRBottomSheetModel
import com.ueno.cia.feature.qr.data.QRCodeModel
import com.ueno.cia.feature.qr.data.QRConfirmRequest
import com.ueno.cia.feature.qr.data.QRGenerateRequest
import com.ueno.cia.feature.qr.data.QRGenerateResponse
import com.ueno.cia.feature.qr.data.QROptionModel
import com.ueno.cia.feature.qr.data.QRScreenModel
import com.ueno.cia.feature.qr.domain.QRGenerateUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Base64
import kotlin.coroutines.cancellation.CancellationException

/** Equivale a `QrGenerateState`. */
data class QRViewState(
    val qrScreen: QRScreenModel,
    /** PNG ya decodificado del base64 del BFF. */
    val qrCodeImage: ByteArray,
    val amountValue: String,
    val currentProvider: ChargeProvider,
    val selectedOptionLabel: String,
) {
    val options: List<QROptionModel> get() = qrScreen.bottomSheet?.options.orEmpty()

    /** Solo se muestra el selector si hay más de un proveedor. */
    val selectorEnabled: Boolean get() = options.size > 1
}

sealed interface QRState {
    data object Loading : QRState
    data class Loaded(val value: QRViewState) : QRState
    data object Failed : QRState
}

sealed interface QRGenerateDestination {
    /** `pushReplacement`: timeout o error al generar. */
    data class Replace(val route: CIARoute) : QRGenerateDestination

    /** `go`: pago confirmado, el stack de CIA queda solo con la pantalla de resultado. */
    data class Reset(val route: CIARoute) : QRGenerateDestination
    data class ProviderSheet(val sheet: QRBottomSheetModel) : QRGenerateDestination
    data object Back : QRGenerateDestination
}

/** Equivale a `QrGenerateNotifier` (qr_generate_notifier.dart) + `QrAnalytics`. */
class QRGenerateViewModel(
    private val input: QRFlowInput,
    private val useCase: QRGenerateUseCase,
    private val connectivity: ConnectivityChecking,
    private val analytics: Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow<QRState>(QRState.Loading)
    val state: StateFlow<QRState> = _state.asStateFlow()

    private val _destinations = Channel<QRGenerateDestination>(Channel.BUFFERED)
    val destinations: Flow<QRGenerateDestination> = _destinations.receiveAsFlow()

    private var provider: ChargeProvider = input.provider
    private var loadJob: Job? = null
    private var started = false
    private val formatter = CurrencyFormatter.Gs

    private val loaded: QRViewState? get() = (_state.value as? QRState.Loaded)?.value

    fun start() {
        if (started) return
        started = true
        load()
    }

    // region Carga (`_init`)

    fun load() {
        // Al recargar (cambio de proveedor) se corta también el polling del QR anterior
        loadJob?.cancel()
        _state.value = QRState.Loading
        loadJob = viewModelScope.launch {
            val response = try {
                useCase.generate(QRGenerateRequest(CIACurrency.Gs.code, ChargeType.QR.name, input.amount, provider.raw, input.cpfNumber))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = QRState.Failed
                return@launch
            }
            handleGenerated(response)
        }
    }

    private suspend fun handleGenerated(response: QRGenerateResponse) {
        // El BFF puede rechazar el QR con una pantalla de error propia (ej. CPF que no coincide)
        response.errorScreen?.let {
            _destinations.send(QRGenerateDestination.Replace(CIARoute.Congrats(CongratsInput(it, origin = null))))
            return
        }
        val image = runCatching { Base64.getMimeDecoder().decode(response.qrScreen.qrCode.image) }.getOrNull()
        if (image == null || image.isEmpty()) {
            _state.value = QRState.Failed
            return
        }
        _state.value = QRState.Loaded(
            QRViewState(
                qrScreen = response.qrScreen,
                qrCodeImage = image,
                amountValue = TemplateString.fill(
                    response.qrScreen.summaryInfo.amountValueTemplate,
                    mapOf("amount" to formatter.string(input.amount)),
                ),
                currentProvider = provider,
                selectedOptionLabel = response.qrScreen.providerLabel(provider),
            ),
        )
        poll(response.qrScreen.qrCode, response.timeoutScreen)
    }

    // endregion

    // region Polling (`_pollingConfirmPayment`)

    /**
     * Consulta la confirmación cada `interval` segundos (solo con red y sin solapar consultas) hasta que llegue un
     * resultado o venza el QR (`timeout`). Gana el primero que ocurra. Se corta solo al salir de la pantalla:
     * el ViewModel cancela su scope.
     */
    private suspend fun poll(code: QRCodeModel, timeoutScreen: QRTimeoutModel) {
        val pollingProvider = provider
        val request = QRConfirmRequest(input.chargeType, pollingProvider, code.referenceCode)
        val confirmed = withTimeoutOrNull(code.timeout * 1000L) {
            var result: CongratsConfirmResponse? = null
            while (result == null) {
                delay(code.interval * 1000L)
                if (!connectivity.isConnected) continue
                result = try {
                    useCase.confirm(request).takeIf { it.hasResponse }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    // Un confirm fallido no corta el polling: se vuelve a intentar en el próximo ciclo
                    trackConfirmError(error, pollingProvider)
                    null
                }
            }
            result
        }
        handle(confirmed, timeoutScreen, pollingProvider)
    }

    private suspend fun handle(confirmed: CongratsConfirmResponse?, timeoutScreen: QRTimeoutModel, provider: ChargeProvider) {
        if (confirmed == null) {
            analytics.flowSuccess(
                stepName = "qr_timeout",
                previousStepName = QRAnalyticsNames.INPUT_AMOUNT_STEP,
                status = "error",
                subFlow = QRAnalyticsNames.subFlow(provider),
                extra = mapOf(Analytics.SCREEN_NAME_KEY to TIMEOUT_SCREEN_NAME),
            )
            _destinations.send(QRGenerateDestination.Replace(CIARoute.QrTimeout(timeoutScreen)))
            return
        }
        val screen = confirmed.responseScreen ?: return
        val isSuccess = screen.data.semanticStatus == "success"
        analytics.flowSuccess(
            stepName = "qr_success",
            previousStepName = "escanear_codigo",
            status = if (isSuccess) "ok" else "error",
            subFlow = QRAnalyticsNames.subFlow(provider),
            extra = mapOf(Analytics.SCREEN_NAME_KEY to if (isSuccess) SUCCESS_SCREEN_NAME else ERROR_SCREEN_NAME),
        )
        _destinations.send(
            QRGenerateDestination.Reset(CIARoute.Congrats(CongratsInput(screen, CongratsOrigin.QR, confirmed.receiptScreen))),
        )
    }

    private fun trackConfirmError(error: Throwable, provider: ChargeProvider) {
        val (code, message) = if (error is APIError.Server) {
            (error.code?.toIntOrNull() ?: -1) to error.serverMessage.orEmpty()
        } else {
            -1 to error.toString()
        }
        analytics.error(
            serviceName = com.ueno.cia.core.CIAEndpoints.QR.CONFIRM,
            errorCode = code,
            errorMessage = message,
            subFlow = QRAnalyticsNames.subFlow(provider),
            extra = mapOf(Analytics.SCREEN_NAME_KEY to SCREEN_NAME),
        )
    }

    // endregion

    // region Acciones del usuario

    /** `handleEditQr`: avisa a /events, mide el tap y vuelve al monto. */
    fun didTapEdit() {
        val state = loaded ?: return
        viewModelScope.launch { useCase.notifyEditQR() }
        analytics.buttonTap(
            label = state.qrScreen.secondaryButton.text,
            destination = INPUT_AMOUNT_PATH,
            subFlow = QRAnalyticsNames.subFlow(state.currentProvider),
            extra = mapOf(Analytics.SCREEN_NAME_KEY to SCREEN_NAME),
        )
        _destinations.trySend(QRGenerateDestination.Back)
    }

    fun didTapBack() {
        analytics.buttonTap(
            label = "back",
            destination = INPUT_AMOUNT_PATH,
            subFlow = QRAnalyticsNames.subFlow(provider),
            extra = mapOf(Analytics.SCREEN_NAME_KEY to SCREEN_NAME),
        )
        _destinations.trySend(QRGenerateDestination.Back)
    }

    /** `handleShareQr` (la captura y la hoja de compartir las resuelve la vista). */
    fun didTapShare() {
        val state = loaded ?: return
        analytics.buttonTap(
            label = state.qrScreen.primaryButton?.text.orEmpty(),
            destination = SHARE_DESTINATION,
            subFlow = QRAnalyticsNames.subFlow(state.currentProvider),
            extra = mapOf(Analytics.SCREEN_NAME_KEY to SCREEN_NAME),
        )
    }

    fun didTapProviderSelector() {
        val sheet = loaded?.qrScreen?.bottomSheet ?: return
        _destinations.trySend(QRGenerateDestination.ProviderSheet(sheet))
    }

    /** `handleProviderSelection` + `generateNewQr`: no hace nada si es el mismo proveedor. */
    fun didSelectProvider(newProvider: ChargeProvider) {
        val state = loaded ?: return
        if (state.currentProvider == newProvider) return
        provider = newProvider
        load()
    }

    fun didTapBackFromError() {
        _destinations.trySend(QRGenerateDestination.Back)
    }

    // endregion

    private companion object {
        const val SCREEN_NAME = "cobros_qr_escaner_codigo"
        const val TIMEOUT_SCREEN_NAME = "cobros_qr_timeout_screen"
        const val SUCCESS_SCREEN_NAME = "cobros_qr_success"
        const val ERROR_SCREEN_NAME = "cobros_qr_error"
        const val SHARE_DESTINATION = "qr_share_sheet"
        const val INPUT_AMOUNT_PATH = "/smart_charge/tap_to_phone/input_amount"
    }
}
