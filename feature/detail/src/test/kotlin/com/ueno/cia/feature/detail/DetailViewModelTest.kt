package com.ueno.cia.feature.detail

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.network.APIError
import com.ueno.cia.designsystem.components.StepperStep
import com.ueno.cia.designsystem.components.stepperActiveIndex
import com.ueno.cia.feature.detail.data.DetailResponse
import com.ueno.cia.feature.detail.domain.DetailUseCase
import com.ueno.cia.feature.detail.ui.DetailDestination
import com.ueno.cia.feature.detail.ui.DetailUiState
import com.ueno.cia.feature.detail.ui.DetailViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = mutableListOf<AnalyticsEvent>()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val response = Fixtures.decode(DetailResponse.serializer(), "detail", "bff_get_detail_response_v3.json")

    private class FakeDetail(val response: DetailResponse) : DetailUseCase {
        val calls = mutableListOf<Pair<String, String?>>()
        var fail = false
        override suspend fun getDetail(ticketNumber: String, channel: String?): DetailResponse {
            calls += ticketNumber to channel
            if (fail) throw APIError.Server(500, null, "boom")
            return response
        }
    }

    private fun viewModel(useCase: FakeDetail) =
        DetailViewModel("351", "upos", useCase, Analytics(AnalyticsTracking { events += it }))

    @Test
    fun `el fixture vigente del detalle se decodifica`() {
        // Los fixtures v1 y v2 son del contrato anterior (ni Flutter ni iOS los usan en el sandbox)
        Fixtures.decode(DetailResponse.serializer(), "detail", "bff_get_detail_response_v3.json")
    }

    @Test
    fun `pide el detalle por ticket y canal`() = runTest(dispatcher) {
        val useCase = FakeDetail(response)
        val vm = viewModel(useCase)
        vm.start(); advanceUntilIdle()
        assertEquals(listOf<Pair<String, String?>>("351" to "upos"), useCase.calls)
        assertIs<DetailUiState.Loaded>(vm.state.value)
    }

    @Test
    fun `si falla muestra el error y se puede reintentar`() = runTest(dispatcher) {
        val useCase = FakeDetail(response).apply { fail = true }
        val vm = viewModel(useCase)
        vm.start(); advanceUntilIdle()
        assertEquals(DetailUiState.Failed, vm.state.value)
        useCase.fail = false
        vm.load()
        assertEquals(DetailUiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertIs<DetailUiState.Loaded>(vm.state.value)
    }

    @Test
    fun `compartir el comprobante abre el recibo y mide el tap`() = runTest(dispatcher) {
        val vm = viewModel(FakeDetail(response))
        vm.start(); advanceUntilIdle()
        vm.didTapShareReceipt()
        val destination = vm.destinations.first()
        assertIs<DetailDestination.Push>(destination)
        assertIs<CIARoute.Receipt>(destination.route)
        val event = events.single { it.kind == AnalyticsEvent.Kind.BUTTON_TAP }
        assertEquals("detalle", event.subFlow)
        assertEquals("Compartir comprobante", event.parameters["eventLabel"])
    }

    @Test
    fun `sin comprobante compartir no hace nada`() = runTest(dispatcher) {
        val vm = viewModel(FakeDetail(response.copy(receiptScreen = null)))
        vm.start(); advanceUntilIdle()
        vm.didTapShareReceipt()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `solo el link COPY copia y avisa`() = runTest(dispatcher) {
        val vm = viewModel(FakeDetail(response))
        vm.didTapActionLink("OTRA", "x")
        vm.didTapActionLink("COPY", "123")
        assertEquals(DetailDestination.Copy("123", DetailViewModel.COPY_MESSAGE), vm.destinations.first())
    }

    @Test
    fun `el paso activo del stepper`() {
        fun step(status: String) = StepperStep("t", null, status)
        assertEquals(1, stepperActiveIndex(listOf(step("completed"), step("info"), step("disabled"))))
        assertEquals(1, stepperActiveIndex(listOf(step("completed"), step("completed"), step("disabled"))))
        assertEquals(0, stepperActiveIndex(listOf(step("disabled"), step("disabled"))))
    }

    @Test
    fun `el fixture trae tarjeta de descuentos y un paso`() {
        val screen = response.detailScreen
        assertNotNull(screen.discountsCard)
        assertEquals(1, screen.stepperCard.items.size)
    }
}
