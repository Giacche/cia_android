package com.ueno.cia.feature.link

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsConfirmDto
import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.model.DetailCardItemModel
import com.ueno.cia.core.model.TycModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.LinkFlowInput
import com.ueno.cia.core.network.APIError
import com.ueno.cia.core.network.CIAJson
import com.ueno.cia.feature.link.data.LinkGenerateRequest
import com.ueno.cia.feature.link.data.LinkListItemModel
import com.ueno.cia.feature.link.data.LinkListResponse
import com.ueno.cia.feature.link.domain.LinkGenerateUseCase
import com.ueno.cia.feature.link.domain.LinkListUseCase
import com.ueno.cia.feature.link.ui.LinkGenerateDestination
import com.ueno.cia.feature.link.ui.LinkGenerateState
import com.ueno.cia.feature.link.ui.LinkGenerateViewModel
import com.ueno.cia.feature.link.ui.LinkListDestination
import com.ueno.cia.feature.link.ui.LinkListUiState
import com.ueno.cia.feature.link.ui.LinkListViewModel
import com.ueno.cia.feature.link.ui.LinkPreConfirmDestination
import com.ueno.cia.feature.link.ui.LinkPreConfirmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import java.io.File
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

@Serializable
private data class PreConfirmOnly(val linkPreConfirmScreen: com.ueno.cia.core.model.LinkPreConfirmModel? = null)

private class FakeLinkList(val first: LinkListResponse, val empty: LinkListResponse) : LinkListUseCase {
    val calls = mutableListOf<Triple<String, Int, Int>>()
    var fail = false
    var pageItems: (String, Int) -> List<LinkListItemModel>? = { _, _ -> null }

    override suspend fun list(status: String, page: Int, size: Int): LinkListResponse {
        calls += Triple(status, page, size)
        if (fail) throw APIError.Server(500, null, "boom")
        val items = pageItems(status, page) ?: return first
        return first.copy(linkListScreen = first.linkListScreen!!.copy(tabItems = items))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LinkViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = mutableListOf<AnalyticsEvent>()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val list = Contratos.decode(LinkListResponse.serializer(), "link", "bff_link_get_list_response.json")
    private val emptyList = Contratos.decode(LinkListResponse.serializer(), "link", "bff_link_get_list_empty_state_response.json")
    private fun item(n: Int) = LinkListItemModel("Link $n", "Gs. $n")

    private fun listViewModel(useCase: FakeLinkList) = LinkListViewModel(useCase, Analytics(AnalyticsTracking { events += it }))
    private fun LinkListViewModel.loaded() = (state.value as LinkListUiState.Loaded).value

    // region Mis links

    @Test
    fun `los contratos de la lista y de generar se decodifican`() {
        assertNotNull(list.linkListScreen)
        assertNotNull(emptyList.linkListScreen)
        val raw = File("../../app/src/main/assets/contratos/link/bff_link_post_generate_response.json").readText()
        val data = CIAJson.parseToJsonElement(raw).let { (it as kotlinx.serialization.json.JsonObject)["data"].toString() }
        assertTrue(CIAJson.decodeFromString(CongratsConfirmDto.serializer(), data).toResponse().hasResponse)
    }

    @Test
    fun `carga los activos y deja elegida la pestana del BFF`() = runTest(dispatcher) {
        val useCase = FakeLinkList(list, emptyList)
        val vm = listViewModel(useCase)
        vm.start()
        advanceUntilIdle()
        assertEquals(Triple("ACTIVE", 0, 10), useCase.calls.single())
        assertEquals(0, vm.loaded().currentTabIndex)
        assertEquals(2, vm.loaded().items.size)
        assertEquals("mis_links", events.single { it.kind == AnalyticsEvent.Kind.CUSTOM_SCREEN_VIEW }.extraParams["screen_name"])
    }

    @Test
    fun `una lista corta es la ultima pagina`() = runTest(dispatcher) {
        val vm = listViewModel(FakeLinkList(list, emptyList))
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.loaded().isLastPage)
    }

    @Test
    fun `si falla la carga inicial muestra el error`() = runTest(dispatcher) {
        val vm = listViewModel(FakeLinkList(list, emptyList).apply { fail = true })
        vm.start()
        advanceUntilIdle()
        assertEquals(LinkListUiState.Failed, vm.state.value)
    }

    @Test
    fun `cambiar de pestana pide los links de ese estado`() = runTest(dispatcher) {
        val useCase = FakeLinkList(list, emptyList)
        val vm = listViewModel(useCase)
        vm.start()
        advanceUntilIdle()
        val expired = vm.loaded().tabs.indexOfFirst { it.id == "EXPIRED" }
        vm.didSelectTab(expired)
        assertTrue(vm.loaded().isLoadingItems)
        advanceUntilIdle()
        assertEquals(Triple("EXPIRED", 0, 10), useCase.calls.last())
        assertFalse(vm.loaded().isLoadingItems)
        assertEquals(expired, vm.loaded().currentTabIndex)
    }

    @Test
    fun `elegir la misma pestana no vuelve a pedir`() = runTest(dispatcher) {
        val useCase = FakeLinkList(list, emptyList)
        val vm = listViewModel(useCase)
        vm.start()
        advanceUntilIdle()
        vm.didSelectTab(0)
        advanceUntilIdle()
        assertEquals(1, useCase.calls.size)
    }

    @Test
    fun `la paginacion de a 10 agrega y corta con una pagina corta`() = runTest(dispatcher) {
        val useCase = FakeLinkList(list, emptyList).apply {
            pageItems = { _, page ->
                if (page == 0) {
                    List(10) {
                        item(it)
                    }
                } else if (page == 1) List(10) { item(100 + it) } else List(3) { item(200 + it) }
            }
        }
        val vm = listViewModel(useCase)
        vm.start()
        advanceUntilIdle()
        assertFalse(vm.loaded().isLastPage)

        vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(20, vm.loaded().items.size)
        assertFalse(vm.loaded().isLastPage)

        vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(23, vm.loaded().items.size)
        assertTrue(vm.loaded().isLastPage)

        vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(3, useCase.calls.size)
    }

    @Test
    fun `cerrar el mensaje lo quita de la pestana actual`() = runTest(dispatcher) {
        val vm = listViewModel(FakeLinkList(list, emptyList))
        vm.start()
        advanceUntilIdle()
        val withMessage = vm.loaded().tabs.indexOfFirst { it.message != null }
        vm.didSelectTab(withMessage)
        advanceUntilIdle()
        assertNotNull(vm.loaded().currentTab?.message)
        vm.closeMessage()
        assertNull(vm.loaded().currentTab?.message)
    }

    @Test
    fun `crear un link abre validar limite y compartir comparte el contenido`() = runTest(dispatcher) {
        val vm = listViewModel(FakeLinkList(list, emptyList))
        vm.start()
        advanceUntilIdle()
        vm.didTapContinue()
        assertEquals(
            LinkListDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.LINK, ChargeProvider.LINK)),
            vm.destinations.first(),
        )
        val shareable = vm.loaded().items.first { it.actionButton != null }
        vm.didTapShare(shareable)
        assertEquals(LinkListDestination.Share(shareable.actionButton!!.content), vm.destinations.first())
        vm.didTapGoBackHome()
        assertEquals(LinkListDestination.GoBackToHome, vm.destinations.first())
    }

    // endregion

    // region Pre-confirmación

    private fun flowInput(withTyc: Boolean): LinkFlowInput {
        val raw = File("../../app/src/main/assets/contratos/validate/bff_get_validate_link_response.json").readText()
        val screen = Contratos.decode(PreConfirmOnly.serializer(), "validate", "bff_get_validate_link_response.json").linkPreConfirmScreen!!
        return LinkFlowInput(
            chargeType = ChargeType.LINK,
            provider = ChargeProvider.LINK,
            amount = BigDecimal(50000),
            amountFormatted = "Gs. 50.000",
            description = "Café",
            approximateToReceive = "Gs. 47.250",
            linkPreConfirmScreen = screen.copy(
                tyc = if (withTyc) TycModel("cia_tyc", "Acepto los", "Términos y condiciones", "https://ueno.com.py/tyc") else null,
            ),
            extraDetailItems = listOf(DetailCardItemModel("Descripción", "Café"), DetailCardItemModel("Total a cobrar", "Gs. 50.000")),
        ).also { assertTrue(raw.isNotEmpty()) }
    }

    @Test
    fun `une los items del BFF con los que arma el monto`() {
        val vm = LinkPreConfirmViewModel(flowInput(withTyc = false))
        val labels = vm.state.value.screen.detailCard.items!!.map { it.label }
        assertEquals(listOf("Descripción", "Total a cobrar"), labels.takeLast(2))
    }

    @Test
    fun `con terminos el boton se habilita al marcar la casilla`() {
        val vm = LinkPreConfirmViewModel(flowInput(withTyc = true))
        assertNotNull(vm.state.value.tyc)
        assertFalse(vm.state.value.buttonEnabled)
        vm.didToggleAccepted()
        assertTrue(vm.state.value.buttonEnabled)
        vm.didToggleAccepted()
        assertFalse(vm.state.value.buttonEnabled)
    }

    @Test
    fun `sin terminos el boton ya esta habilitado`() {
        assertTrue(LinkPreConfirmViewModel(flowInput(withTyc = false)).state.value.buttonEnabled)
    }

    @Test
    fun `continuar genera el link y los terminos abren el documento`() = runTest(dispatcher) {
        val input = flowInput(withTyc = true)
        val vm = LinkPreConfirmViewModel(input)
        vm.didTapContinue()
        // Sin aceptar no avanza
        vm.didTapTyc()
        assertEquals(LinkPreConfirmDestination.OpenDocument(input.linkPreConfirmScreen.tyc!!.linkUrl), vm.destinations.first())
        vm.didToggleAccepted()
        vm.didTapContinue()
        assertEquals(LinkPreConfirmDestination.Push(CIARoute.LinkGenerate(input)), vm.destinations.first())
    }

    // endregion

    // region Generar

    private class FakeGenerate(var result: () -> CongratsConfirmResponse) : LinkGenerateUseCase {
        var request: LinkGenerateRequest? = null
        override suspend fun generate(request: LinkGenerateRequest): CongratsConfirmResponse {
            this.request = request
            return result()
        }
    }

    private fun generated(): CongratsConfirmResponse {
        val raw = File("../../app/src/main/assets/contratos/link/bff_link_post_generate_response.json").readText()
        val data = CIAJson.parseToJsonElement(raw).let { (it as kotlinx.serialization.json.JsonObject)["data"].toString() }
        return CIAJson.decodeFromString(CongratsConfirmDto.serializer(), data).toResponse()
    }

    @Test
    fun `generar manda monto y descripcion y resetea al resultado`() = runTest(dispatcher) {
        val useCase = FakeGenerate { generated() }
        val vm = LinkGenerateViewModel(flowInput(withTyc = false), useCase)
        vm.start()
        advanceUntilIdle()
        assertEquals("GS", useCase.request!!.currency)
        assertEquals("LINK", useCase.request!!.type)
        assertEquals(BigDecimal(50000), useCase.request!!.amount)
        assertEquals("Café", useCase.request!!.description)
        val destination = vm.destinations.first()
        assertIs<LinkGenerateDestination.Reset>(destination)
        val route = assertIs<CIARoute.Congrats>(destination.route)
        assertEquals(CongratsOrigin.LINK, route.input.origin)
    }

    @Test
    fun `sin pantalla de resultado o con error falla`() = runTest(dispatcher) {
        val empty = LinkGenerateViewModel(flowInput(false), FakeGenerate { CongratsConfirmResponse(null, null) })
        empty.start()
        advanceUntilIdle()
        assertEquals(LinkGenerateState.Failed, empty.state.value)

        val failing = LinkGenerateViewModel(flowInput(false), FakeGenerate { throw APIError.Server(500, null, "boom") })
        failing.start()
        advanceUntilIdle()
        assertEquals(LinkGenerateState.Failed, failing.state.value)
        failing.didTapBackFromError()
        assertEquals(LinkGenerateDestination.Back, failing.destinations.first())
    }

    // endregion
}
