package com.ueno.cia.feature.home.ui

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.analytics.HomeAnalytics
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.network.APIError
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.core.tenant.TenantContext
import com.ueno.cia.feature.home.Contratos
import com.ueno.cia.feature.home.data.HomeAlertsResponse
import com.ueno.cia.feature.home.data.HomeBannersResponse
import com.ueno.cia.feature.home.data.HomeHeaderResponse
import com.ueno.cia.feature.home.data.HomeLayoutResponse
import com.ueno.cia.feature.home.data.HomeOptionsResponse
import com.ueno.cia.feature.home.data.HomePendingSettlementResponse
import com.ueno.cia.feature.home.data.HomeRecentsResponse
import com.ueno.cia.feature.home.domain.HomeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Use case que responde con los contratos reales del BFF; cada llamada puede fallar a pedido. */
private class FakeHomeUseCase(
    layoutContrato: String = "home_get_home_layout.json",
    headerContrato: String = "home_get_home_header.json",
    optionsContrato: String = "home_get_home_options.json",
) : HomeUseCase {
    val failing = mutableSetOf<String>()
    val calls = mutableListOf<String>()

    private val layout = Contratos.decode(HomeLayoutResponse.serializer(), "home", layoutContrato)
    private val header = Contratos.decode(HomeHeaderResponse.serializer(), "home", headerContrato)
    private val options = Contratos.decode(HomeOptionsResponse.serializer(), "home", optionsContrato)
    private val banners = Contratos.decode(HomeBannersResponse.serializer(), "home", "home_get_home_banners.json")
    private val recents = Contratos.decode(HomeRecentsResponse.serializer(), "home", "home_get_home_recents.json")
    private val alerts = Contratos.decode(HomeAlertsResponse.serializer(), "home", "home_get_home_alerts.json")
    private val pending = Contratos.decode(HomePendingSettlementResponse.serializer(), "home", "home_get_home_pending_settlement.json")
    private val charges = Contratos.decode(MovementsResponse.serializer(), "charges", "bff_charges_tabs_cobros_get_response.json")

    private fun <T> respond(name: String, value: T): T {
        calls += name
        if (name in failing) throw APIError.Server(500, null, "boom")
        return value
    }

    override suspend fun layout() = respond("layout", layout)
    override suspend fun header() = respond("header", header)
    override suspend fun options() = respond("options", options)
    override suspend fun banners() = respond("banners", banners)
    override suspend fun recents() = respond("recents", recents)
    override suspend fun alerts() = respond("alerts", alerts)
    override suspend fun pendingSettlement() = respond("pendingSettlement", pending)
    override suspend fun charges(page: Int) = respond("charges", charges)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val events = mutableListOf<AnalyticsEvent>()
    private val tenantContext = TenantContext(Tenant.INDIVIDUOS)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(useCase: HomeUseCase = FakeHomeUseCase()) = HomeViewModel(
        useCase = useCase,
        tenantContext = tenantContext,
        analytics = HomeAnalytics(Analytics(AnalyticsTracking { events += it })),
    )

    private fun HomeViewModel.loaded(): HomeState = (state.value as HomeUiState.Loaded).state

    @Test
    fun `arranca en loading y carga todas las secciones del layout`() = runTest(dispatcher) {
        val vm = viewModel()
        assertIs<HomeUiState.Loading>(vm.state.value)
        vm.start()
        advanceUntilIdle()
        val state = vm.loaded()
        assertIs<Loadable.Loaded<*>>(state.header)
        assertIs<Loadable.Loaded<*>>(state.options)
        assertIs<Loadable.Loaded<*>>(state.banners)
        assertIs<Loadable.Loaded<*>>(state.alerts)
        assertIs<Loadable.Loaded<*>>(state.recents)
        assertIs<Loadable.Loaded<*>>(state.charges)
        assertTrue(state.visibleCharges.size <= HomeState.MAX_VISIBLE_CHARGES)
    }

    @Test
    fun `start no vuelve a pedir todo si ya arranco`() = runTest(dispatcher) {
        val useCase = FakeHomeUseCase()
        val vm = viewModel(useCase)
        vm.start()
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()
        assertEquals(1, useCase.calls.count { it == "layout" })
    }

    @Test
    fun `si falla el layout la pantalla falla entera`() = runTest(dispatcher) {
        val useCase = FakeHomeUseCase().apply { failing += "layout" }
        val vm = viewModel(useCase)
        vm.start()
        advanceUntilIdle()
        assertIs<HomeUiState.Failed>(vm.state.value)
        assertTrue(useCase.calls.none { it == "header" || it == "options" })
    }

    @Test
    fun `una seccion que falla no rompe a las demas y se puede reintentar`() = runTest(dispatcher) {
        val useCase = FakeHomeUseCase().apply { failing += "options" }
        val vm = viewModel(useCase)
        vm.start()
        advanceUntilIdle()
        assertEquals(Loadable.Failed, vm.loaded().options)
        assertIs<Loadable.Loaded<*>>(vm.loaded().header)

        useCase.failing.clear()
        vm.retryOptions()
        assertEquals(Loadable.Loading, vm.loaded().options)
        advanceUntilIdle()
        assertIs<Loadable.Loaded<*>>(vm.loaded().options)
    }

    @Test
    fun `un errorScreen del layout resetea a la pantalla de resultado`() = runTest(dispatcher) {
        val vm = viewModel(FakeHomeUseCase(layoutContrato = "home_get_home_layout_error_empresas.json"))
        vm.start()
        advanceUntilIdle()
        assertIs<HomeUiState.Failed>(vm.state.value)
        val destination = vm.destinations.first()
        assertIs<HomeDestination.Reset>(destination)
        assertIs<CIARoute.Congrats>(destination.route)
    }

    @Test
    fun `refresh mantiene la estructura y apaga el indicador al terminar`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        vm.refresh()
        assertTrue(vm.isRefreshing.value)
        assertEquals(Loadable.Loading, vm.loaded().options)
        advanceUntilIdle()
        assertTrue(!vm.isRefreshing.value)
        assertIs<Loadable.Loaded<*>>(vm.loaded().options)
    }

    @Test
    fun `tocar una opcion de QR navega a validar limite y registra analytics`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        // En el contrato por defecto "QR" trae un modal con los QR por proveedor
        val qr = (vm.loaded().options as Loadable.Loaded).value.options.first { it.id == "QR" }
        vm.didTapOption(qr)
        assertIs<HomeDestination.OptionsSheet>(vm.destinations.first())
        // QR_UENO viene deshabilitado en el contrato; QR_DEPAY está habilitado
        vm.didSelectModalOption(qr.modal!!.options.first { it.id == "QR_DEPAY" })
        val destination = vm.destinations.first()
        assertIs<HomeDestination.Push>(destination)
        assertEquals(CIARoute.ValidateLimitEntryPoint(ChargeType.QR, ChargeProvider.DEPAY), destination.route)
        val event = events.single { it.kind == AnalyticsEvent.Kind.FLOW_START }
        assertEquals("qr_depay_inicio", event.parameters["stepName"])
        assertEquals("cobros_home", event.extraParams["screen_name"])
    }

    @Test
    fun `una opcion de un modal deshabilitada no navega`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        val qr = (vm.loaded().options as Loadable.Loaded).value.options.first { it.id == "QR" }
        vm.didSelectModalOption(qr.modal!!.options.first { it.id == "QR_UENO" && it.disabled })
        advanceUntilIdle()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `una opcion deshabilitada no navega`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        val option = (vm.loaded().options as Loadable.Loaded).value.options.first().copy(disabled = true)
        vm.didTapOption(option)
        advanceUntilIdle()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `una opcion con modal abre el bottom sheet`() = runTest(dispatcher) {
        val vm = viewModel(FakeHomeUseCase(optionsContrato = "home_get_home_options_qrs_inter.json"))
        vm.start()
        advanceUntilIdle()
        val withModal = (vm.loaded().options as Loadable.Loaded).value.options.firstOrNull { it.modal != null }
        assertNotNull(withModal, "El contrato de QR internacional debería traer una opción con modal")
        vm.didTapOption(withModal)
        assertIs<HomeDestination.OptionsSheet>(vm.destinations.first())
    }

    @Test
    fun `Android mantiene la opcion TTP`() {
        val options = Contratos.decode(HomeOptionsResponse.serializer(), "home", "home_get_home_options.json")
        val withTtp = options.copy(options = options.options + options.options.first().copy(id = "TTP"))
        val content = HomeViewModel.optionsContent(withTtp)
        assertTrue(content.options.any { it.id == "TTP" })
    }

    @Test
    fun `sin opciones se muestra el mensaje EMPTY_OPTIONS`() {
        val options = Contratos.decode(HomeOptionsResponse.serializer(), "home", "home_get_home_options.json")
        val message = options.messages.firstOrNull() ?: com.ueno.cia.feature.home.data.HomeMessageModel(
            id = "EMPTY_OPTIONS", type = "information", message = "Sin opciones", description = "",
        )
        val content = HomeViewModel.optionsContent(
            options.copy(options = emptyList(), messages = listOf(message.copy(id = "EMPTY_OPTIONS"))),
        )
        assertTrue(content.options.isEmpty())
        assertEquals("EMPTY_OPTIONS", content.message?.id)
    }

    @Test
    fun `deslizar una sucursal fija el tenant y recarga lo que depende de ella`() = runTest(dispatcher) {
        val useCase = FakeHomeUseCase(
            layoutContrato = "home_get_home_layout_individuos_carousel.json",
            headerContrato = "home_get_home_header_individuos_carousel.json",
        )
        val vm = viewModel(useCase)
        vm.start()
        advanceUntilIdle()
        val branch = vm.loaded().header.valueOrNull!!.branchCarousel!!.branches.first()
        val optionsBefore = useCase.calls.count { it == "options" }

        vm.didSwipeBranch(branch)
        assertEquals(branch.branchId, tenantContext.snapshot.activeBranchId)
        assertEquals(Loadable.Loading, vm.loaded().options)
        advanceUntilIdle()

        assertEquals(optionsBefore + 1, useCase.calls.count { it == "options" })
        assertEquals(branch.branchId, vm.loaded().selectedBranchId)
        assertIs<Loadable.Loaded<*>>(vm.loaded().options)
    }

    @Test
    fun `llegar a la pagina Mostrar todas deshabilita opciones y oculta banners y recientes`() = runTest(dispatcher) {
        val vm = viewModel(
            FakeHomeUseCase(
                layoutContrato = "home_get_home_layout_individuos_carousel.json",
                headerContrato = "home_get_home_header_individuos_carousel.json",
            ),
        )
        vm.start()
        advanceUntilIdle()
        vm.didReachShowAllBranchesPage()
        val state = vm.loaded()
        assertTrue(state.isBranchSelectionActive)
        assertTrue(!state.showsBanners)
        assertTrue(!state.showsRecents)
        assertEquals(Loadable.Loading, state.pendingSettlement)
    }

    @Test
    fun `ver todos los movimientos navega con la pantalla de historial`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        vm.didTapShowAllMovements()
        val destination = vm.destinations.first()
        assertIs<HomeDestination.Push>(destination)
        assertIs<CIARoute.Movements>(destination.route)
    }

    @Test
    fun `tocar un movimiento abre su detalle`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()
        val charge = vm.loaded().visibleCharges.first()
        vm.didTapMovement(charge)
        val destination = vm.destinations.first()
        assertEquals(HomeDestination.Push(CIARoute.Detail(ticketNumber = charge.ticketNumber)), destination)
    }

    @Test
    fun `volver sale de CIA`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.didTapBack()
        assertEquals(HomeDestination.Exit, vm.destinations.first())
    }

    @Test
    fun `sin datos del header el perfil y la ayuda no hacen nada`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.didTapProfile()
        vm.didTapHelp()
        advanceUntilIdle()
        assertNull(null)
        assertTrue(events.isEmpty())
    }
}
