package com.ueno.cia.feature.paymentselector

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.analytics.HomeAnalytics
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.PaymentSelectorResponse
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.PaymentSelectorInput
import com.ueno.cia.core.network.APIError
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.core.network.HttpClient
import com.ueno.cia.core.network.HttpRequest
import com.ueno.cia.core.network.HttpResponse
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.core.tenant.TenantContext
import com.ueno.cia.core.events.NoOpEvents
import com.ueno.cia.feature.paymentselector.data.PaymentSelectorRepository
import com.ueno.cia.feature.paymentselector.domain.PaymentSelectorUseCase
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorChargeNavigator
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorCountryState
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorCountryViewModel
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorDestination
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorEntryPointViewModel
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorViewModel
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class FakeSelector(var response: PaymentSelectorResponse?) : PaymentSelectorUseCase {
    val calls = mutableListOf<Triple<String?, String?, String?>>()
    override suspend fun entryPoint(xType: String?, branchId: String?, origin: String?): PaymentSelectorResponse {
        calls += Triple(xType, branchId, origin)
        return response ?: throw APIError.Server(500, null, "boom")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentSelectorTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = mutableListOf<AnalyticsEvent>()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun contrato(name: String) = Contratos.decode(PaymentSelectorResponse.serializer(), "payment_selector", name)
    private val all = contrato("ios_get_payment_selector_todos_con_qr_internacional_response.json")
    private val countries = contrato("bff_get_payment_selector_qr_inter_response.json")
    private val withAlert = contrato("bff_get_payment_selector_response.json")
    private val navigator get() = PaymentSelectorChargeNavigator(HomeAnalytics(Analytics(AnalyticsTracking { events += it })))

    @Test
    fun `todos los contratos del selector se decodifican`() {
        File("../../app/src/main/assets/contratos/payment_selector").listFiles()!!.filter { it.extension == "json" }.forEach {
            contrato(it.name)
        }
    }

    // region Entry point

    @Test
    fun `el entry point pide las opciones y reemplaza por el selector`() = runTest(dispatcher) {
        val useCase = FakeSelector(all)
        val vm = PaymentSelectorEntryPointViewModel("QR", "B1", "SELECTOR_BUSINESS", useCase)
        vm.start(); advanceUntilIdle()
        assertEquals(Triple<String?, String?, String?>("QR", "B1", "SELECTOR_BUSINESS"), useCase.calls.single())
        val destination = vm.destinations.first()
        assertIs<PaymentSelectorDestination.Replace>(destination)
        val route = assertIs<CIARoute.PaymentSelector>(destination.route)
        assertEquals("B1", route.input.branchId)
        assertEquals(all.paymentEntryPoint, route.input.screen)
    }

    @Test
    fun `sin pantalla en la respuesta o con error falla y volver sale`() = runTest(dispatcher) {
        val empty = PaymentSelectorEntryPointViewModel(null, null, null, FakeSelector(PaymentSelectorResponse(null)))
        empty.start(); advanceUntilIdle()
        assertEquals(PaymentSelectorEntryPointViewModel.State.Failed, empty.state.value)

        val failing = PaymentSelectorEntryPointViewModel(null, null, null, FakeSelector(null))
        failing.start(); advanceUntilIdle()
        assertEquals(PaymentSelectorEntryPointViewModel.State.Failed, failing.state.value)
        failing.didTapBackFromError()
        assertEquals(PaymentSelectorDestination.Back, failing.destinations.first())
    }

    // endregion

    // region Selector

    private fun selector(response: PaymentSelectorResponse, branch: String? = "B1") =
        PaymentSelectorViewModel(PaymentSelectorInput(response.paymentEntryPoint!!, branch, "SELECTOR_BUSINESS"), navigator)

    @Test
    fun `una opcion de QR va a validar limite con canPop y mide el inicio`() = runTest(dispatcher) {
        val vm = selector(all)
        vm.didTapOption(vm.screen.data.options.first { it.id == "QR_PIX" })
        assertEquals(
            PaymentSelectorDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.QR, ChargeProvider.PIX, canPop = true)),
            vm.destinations.first(),
        )
        assertEquals("qr_pix_inicio", events.single { it.kind == AnalyticsEvent.Kind.FLOW_START }.parameters["stepName"])
    }

    @Test
    fun `el link va a validar limite con canPop`() = runTest(dispatcher) {
        val vm = selector(all)
        vm.didTapOption(vm.screen.data.options.first { it.id == "LINK" })
        assertEquals(
            PaymentSelectorDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.LINK, ChargeProvider.LINK, canPop = true)),
            vm.destinations.first(),
        )
    }

    @Test
    fun `QR internacional abre el selector de paises con la sucursal y el origen de empresas`() = runTest(dispatcher) {
        val vm = selector(all, branch = "B7")
        vm.didTapOption(vm.screen.data.options.first { it.id == "QR_INTERNACIONAL" })
        assertEquals(
            PaymentSelectorDestination.Push(CIARoute.PaymentSelectorCountry("B7", "SELECTOR_BUSINESS")),
            vm.destinations.first(),
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `una opcion deshabilitada no navega`() = runTest(dispatcher) {
        val vm = selector(withAlert)
        val disabled = vm.screen.data.options.first { it.disabled }
        vm.didTapOption(disabled)
        advanceUntilIdle()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `la accion de la alerta de RUC abre la validacion`() = runTest(dispatcher) {
        val vm = selector(withAlert)
        vm.didTapAlertAction()
        assertEquals(PaymentSelectorDestination.Push(CIARoute.TaxInfo), vm.destinations.first())
        assertEquals("validacion_ruc_informacion_inicio", events.single().parameters["stepName"])
    }

    // endregion

    // region Países

    @Test
    fun `el selector de paises pide QR_INTERNATIONAL y navega al cobro del pais`() = runTest(dispatcher) {
        val useCase = FakeSelector(countries)
        val vm = PaymentSelectorCountryViewModel("B7", "SELECTOR_BUSINESS", useCase, navigator)
        vm.start(); advanceUntilIdle()
        assertEquals(Triple<String?, String?, String?>("QR_INTERNATIONAL", "B7", "SELECTOR_BUSINESS"), useCase.calls.single())
        val loaded = assertIs<PaymentSelectorCountryState.Loaded>(vm.state.value)
        vm.didTapOption(loaded.screen.data.options.first { it.id == "QR_BOLIVIA" })
        assertEquals(
            PaymentSelectorDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.QR, ChargeProvider.BOLIVIA, canPop = true)),
            vm.destinations.first(),
        )
    }

    @Test
    fun `si no vienen paises el selector falla`() = runTest(dispatcher) {
        val vm = PaymentSelectorCountryViewModel(null, null, FakeSelector(PaymentSelectorResponse(null)), navigator)
        vm.start(); advanceUntilIdle()
        assertEquals(PaymentSelectorCountryState.Failed, vm.state.value)
    }

    // endregion

    // region Repositorio

    private class CapturingHttp(val body: String) : HttpClient {
        var request: HttpRequest? = null
        override suspend fun send(request: HttpRequest): HttpResponse {
            this.request = request
            return HttpResponse(200, body)
        }
    }

    @Test
    fun `el repositorio manda tipo sucursal y origen en headers y el tipo en query`() = runTest(dispatcher) {
        val http = CapturingHttp(File("../../app/src/main/assets/contratos/payment_selector/bff_get_payment_selector_qr_inter_response.json").readText())
        val repository = PaymentSelectorRepository(CIAHttpClient(http, TenantContext(Tenant.EMPRESAS)), NoOpEvents)
        val response = repository.entryPoint("QR_INTERNATIONAL", "B9", "SELECTOR_BUSINESS")
        val request = http.request!!
        assertEquals("/bff/smartcharge/paymentEntryPoint", request.path)
        assertEquals(mapOf("xType" to "QR_INTERNATIONAL"), request.query)
        assertEquals("QR_INTERNATIONAL", request.headers["x-type"])
        assertEquals("B9", request.headers["x-branch-id"])
        assertEquals("SELECTOR_BUSINESS", request.headers["x-origin"])
        assertTrue(response.paymentEntryPoint != null)
    }

    @Test
    fun `sin tipo no manda query ni headers de tipo`() = runTest(dispatcher) {
        val http = CapturingHttp(File("../../app/src/main/assets/contratos/payment_selector/bff_get_payment_selector_response.json").readText())
        PaymentSelectorRepository(CIAHttpClient(http, TenantContext(Tenant.INDIVIDUOS)), NoOpEvents).entryPoint(null, null, null)
        assertTrue(http.request!!.query.isEmpty())
        assertNull(http.request!!.headers["x-type"])
    }

    // endregion
}
