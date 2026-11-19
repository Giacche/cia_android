package com.ueno.cia.feature.qr

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.QRFlowInput
import com.ueno.cia.core.network.APIError
import com.ueno.cia.core.network.CIAJson
import com.ueno.cia.core.platform.ConnectivityChecking
import com.ueno.cia.core.model.CongratsConfirmDto
import com.ueno.cia.feature.qr.data.QRConfirmRequest
import com.ueno.cia.feature.qr.data.QRGenerateRequest
import com.ueno.cia.feature.qr.data.QRGenerateResponse
import com.ueno.cia.feature.qr.domain.QRGenerateUseCase
import com.ueno.cia.feature.qr.ui.QRGenerateDestination
import com.ueno.cia.feature.qr.ui.QRGenerateViewModel
import com.ueno.cia.feature.qr.ui.QRState
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class FakeQRUseCase(var generated: QRGenerateResponse) : QRGenerateUseCase {
    val generateRequests = mutableListOf<QRGenerateRequest>()
    val confirmRequests = mutableListOf<QRConfirmRequest>()
    var generateFails = false
    /** Respuestas sucesivas del confirm; la última se repite. `null` = falla. */
    var confirms: MutableList<CongratsConfirmResponse?> = mutableListOf(EMPTY)
    var notified = 0

    override suspend fun generate(request: QRGenerateRequest): QRGenerateResponse {
        generateRequests += request
        if (generateFails) throw APIError.Server(500, null, "boom")
        return generated
    }

    override suspend fun confirm(request: QRConfirmRequest): CongratsConfirmResponse {
        confirmRequests += request
        val next = if (confirms.size > 1) confirms.removeAt(0) else confirms.first()
        return next ?: throw APIError.Server(500, "E1", "falló")
    }

    override suspend fun notifyEditQR() {
        notified++
    }

    companion object {
        val EMPTY = CongratsConfirmResponse(null, null)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class QRGenerateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = mutableListOf<AnalyticsEvent>()
    private var online = true

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun generated(name: String = "bff_qr_post_generate_response.json") =
        Contratos.decode(QRGenerateResponse.serializer(), "qr", name)

    private fun success() = CIAJson.decodeFromString(
        CongratsConfirmDto.serializer(),
        java.io.File("../../app/src/main/assets/contratos/qr/bff_qr_post_confirm_response_success.json").readText().let {
            // el contrato trae el envelope `{ "data": ... }`
            CIAJson.parseToJsonElement(it).let { root -> (root as kotlinx.serialization.json.JsonObject)["data"].toString() }
        },
    ).toResponse()

    private fun viewModel(useCase: FakeQRUseCase, provider: ChargeProvider = ChargeProvider.UPAY, cpf: String? = null) =
        QRGenerateViewModel(
            input = QRFlowInput(BigDecimal(140000), ChargeType.QR, provider, cpf),
            useCase = useCase,
            connectivity = object : ConnectivityChecking {
                override val isConnected: Boolean get() = online
            },
            analytics = Analytics(AnalyticsTracking { events += it }),
        )

    private fun TestScope.started(useCase: FakeQRUseCase, provider: ChargeProvider = ChargeProvider.UPAY, cpf: String? = null): QRGenerateViewModel {
        val vm = viewModel(useCase, provider, cpf)
        vm.start()
        advanceTimeBy(1)
        return vm
    }

    @Test
    fun `genera el QR con el monto, el proveedor y el CPF`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated())
        val vm = started(useCase, ChargeProvider.BANCARD, cpf = "123")
        val request = useCase.generateRequests.single()
        assertEquals("GS", request.currency)
        assertEquals("QR", request.type)
        assertEquals(BigDecimal(140000), request.amount)
        assertEquals("BANCARD", request.provider)
        assertEquals("123", request.cpfNumber)
        val state = assertIs<QRState.Loaded>(vm.state.value).value
        assertEquals("Gs. 140.000", state.amountValue)
        assertTrue(state.qrCodeImage.isNotEmpty())
        assertTrue(state.selectorEnabled)
    }

    @Test
    fun `si falla generar muestra el error y volver sale`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated()).apply { generateFails = true }
        val vm = started(useCase)
        assertEquals(QRState.Failed, vm.state.value)
        vm.didTapBackFromError()
        assertEquals(QRGenerateDestination.Back, vm.destinations.first())
    }

    @Test
    fun `un errorScreen del BFF reemplaza por la pantalla de resultado sin origen`() = runTest(dispatcher) {
        val vm = started(FakeQRUseCase(generated("bff_qr_post_generate_response_semantic.json")))
        val destination = vm.destinations.first()
        assertIs<QRGenerateDestination.Replace>(destination)
        val route = assertIs<CIARoute.Congrats>(destination.route)
        assertNull(route.input.origin)
    }

    @Test
    fun `un pago confirmado resetea a la pantalla de resultado con el comprobante`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated()).apply { confirms = mutableListOf(FakeQRUseCase.EMPTY, FakeQRUseCase.EMPTY, success()) }
        val vm = started(useCase)
        advanceTimeBy(5_000 * 3 + 1)
        val destination = vm.destinations.first()
        assertIs<QRGenerateDestination.Reset>(destination)
        val route = assertIs<CIARoute.Congrats>(destination.route)
        assertEquals(CongratsOrigin.QR, route.input.origin)
        assertNotNull(route.input.receipt)
        assertEquals(3, useCase.confirmRequests.size)
        assertEquals("509UP1744136743748", useCase.confirmRequests.first().referenceCode)
        val event = events.single { it.kind == AnalyticsEvent.Kind.FLOW_SUCCESS }
        assertEquals("qr_success", event.parameters["stepName"])
        assertEquals("ok", event.parameters["status"])
    }

    @Test
    fun `si vence el QR va a la pantalla de timeout`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated())
        val vm = started(useCase)
        advanceTimeBy(300_000 + 1)
        val destination = vm.destinations.first()
        assertIs<QRGenerateDestination.Replace>(destination)
        assertIs<CIARoute.QrTimeout>(destination.route)
        val event = events.single { it.kind == AnalyticsEvent.Kind.FLOW_SUCCESS }
        assertEquals("qr_timeout", event.parameters["stepName"])
        assertEquals("error", event.parameters["status"])
    }

    @Test
    fun `sin red no consulta la confirmacion`() = runTest(dispatcher) {
        online = false
        val useCase = FakeQRUseCase(generated())
        started(useCase)
        advanceTimeBy(60_000)
        assertTrue(useCase.confirmRequests.isEmpty())
    }

    @Test
    fun `un confirm que falla no corta el polling y se mide`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated()).apply { confirms = mutableListOf(null, null, success()) }
        val vm = started(useCase)
        advanceTimeBy(5_000 * 3 + 1)
        assertIs<QRGenerateDestination.Reset>(vm.destinations.first())
        val errors = events.filter { it.kind == AnalyticsEvent.Kind.ERROR }
        assertEquals(2, errors.size)
        assertEquals("E1".toIntOrNull() ?: -1, errors.first().parameters["errorCode"]!!.toInt())
    }

    @Test
    fun `cambiar de proveedor genera otro QR y corta el polling anterior`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated())
        val vm = started(useCase)
        advanceTimeBy(5_000 + 1)
        assertEquals("UPAY", useCase.confirmRequests.last().provider.raw)

        vm.didSelectProvider(ChargeProvider.BANCARD)
        advanceTimeBy(1)
        assertEquals(2, useCase.generateRequests.size)
        assertEquals("BANCARD", useCase.generateRequests.last().provider)
        advanceTimeBy(5_000 + 1)
        assertEquals("BANCARD", useCase.confirmRequests.last().provider.raw)
        // El polling anterior ya no corre: en 5 s hay una sola consulta nueva
        assertEquals(2, useCase.confirmRequests.size)
    }

    @Test
    fun `elegir el mismo proveedor no hace nada`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated())
        val vm = started(useCase, ChargeProvider.UPAY)
        vm.didSelectProvider(ChargeProvider.UPAY)
        advanceUntilIdle()
        assertEquals(1, useCase.generateRequests.size)
    }

    @Test
    fun `editar el monto avisa a eventos, mide y vuelve`() = runTest(dispatcher) {
        val useCase = FakeQRUseCase(generated())
        val vm = started(useCase)
        vm.didTapEdit()
        advanceTimeBy(1)
        assertEquals(1, useCase.notified)
        assertEquals(QRGenerateDestination.Back, vm.destinations.first())
        assertTrue(events.any { it.kind == AnalyticsEvent.Kind.BUTTON_TAP && it.parameters["eventLabel"] == "Editar monto" })
    }

    @Test
    fun `el selector solo se ofrece con mas de un proveedor`() = runTest(dispatcher) {
        val single = generated().let { it.copy(qrScreen = it.qrScreen.copy(bottomSheet = it.qrScreen.bottomSheet!!.copy(options = it.qrScreen.bottomSheet!!.options!!.take(1)))) }
        val vm = started(FakeQRUseCase(single))
        assertFalse(assertIs<QRState.Loaded>(vm.state.value).value.selectorEnabled)
    }

    @Test
    fun `el proveedor desconocido del BFF cae en UPAY y la etiqueta es la del elegido`() = runTest(dispatcher) {
        val response = generated()
        val vm = started(FakeQRUseCase(response))
        val state = assertIs<QRState.Loaded>(vm.state.value).value
        assertEquals("Cambiar QR", state.selectedOptionLabel)
        assertEquals(ChargeProvider.UPAY, response.qrScreen.bottomSheet!!.options!![0].providerType)
        assertEquals(ChargeProvider.BANCARD, response.qrScreen.bottomSheet!!.options!![1].providerType)
    }

    @Test
    fun `un objeto vacio en el confirm significa todavia sin resultado`() {
        val empty = CIAJson.decodeFromString(CongratsConfirmDto.serializer(), """{"responseScreen":{},"receiptScreen":{}}""").toResponse()
        assertFalse(empty.hasResponse)
        val nulls = CIAJson.decodeFromString(CongratsConfirmDto.serializer(), """{"responseScreen":null,"receiptScreen":null}""").toResponse()
        assertFalse(nulls.hasResponse)
        assertTrue(success().hasResponse)
        assertNotNull(success().receiptScreen)
    }

    @Test
    fun `el body del request lleva documentNumber aunque no haya CPF`() {
        val body = com.ueno.cia.core.network.CIARequestJson.encodeToString(
            QRGenerateRequest.serializer(),
            QRGenerateRequest("GS", "QR", BigDecimal(140000), "UPAY", null),
        )
        assertTrue(body.contains("\"documentNumber\":null"), body)
    }
}
