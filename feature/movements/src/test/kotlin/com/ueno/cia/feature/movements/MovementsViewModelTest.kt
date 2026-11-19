package com.ueno.cia.feature.movements

import com.ueno.cia.core.model.MovementChargeModel
import com.ueno.cia.core.model.MovementsQuery
import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.core.model.MovementsScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.network.APIError
import com.ueno.cia.feature.movements.domain.MovementsUseCase
import com.ueno.cia.feature.movements.ui.MovementsDestination
import com.ueno.cia.feature.movements.ui.MovementsFiltersDestination
import com.ueno.cia.feature.movements.ui.MovementsFiltersViewModel
import com.ueno.cia.feature.movements.ui.MovementsSession
import com.ueno.cia.feature.movements.ui.MovementsUiState
import com.ueno.cia.feature.movements.ui.MovementsViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private fun charge(n: Int) = MovementChargeModel("Cobro $n", "Sub $n", "Gs. $n", "Hoy", "10:00", ticketNumber = "T$n")

private class FakeMovements(var base: MovementsResponse) : MovementsUseCase {
    val queries = mutableListOf<MovementsQuery>()
    var fail = false
    /** Cobros por página; si no hay, se devuelve [base]. */
    var pages: (MovementsQuery) -> List<MovementChargeModel>? = { null }

    override suspend fun getMoves(query: MovementsQuery): MovementsResponse {
        queries += query
        if (fail) throw APIError.Server(500, null, "boom")
        return pages(query)?.let { base.copy(charges = it) } ?: base
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MovementsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val cobros = Contratos.decode(MovementsResponse.serializer(), "charges", "bff_charges_tabs_cobros_get_response.json")
    private val withFilters = Contratos.decode(MovementsResponse.serializer(), "charges", "bff_charges_filters_get_response.json")
    private val screen = MovementsScreenModel("HISTORY", "cia_movements", "MOVIMIENTOS", "Movimientos", pageSize = 3)

    private fun viewModel(useCase: FakeMovements, session: MovementsSession = MovementsSession(screen.pageSize)) =
        MovementsViewModel(screen, useCase, session)

    private fun MovementsViewModel.loaded() = (state.value as MovementsUiState.Loaded).value

    @Test
    fun `los contratos de cobros y de filtros se decodifican`() {
        listOf(
            "bff_charges_tabs_cobros_get_response.json",
            "bff_charges_tabs_servicios_get_response.json",
            "bff_charges_tabs_servicios_filters_get_response.json",
            "bff_charges_filters_get_response.json",
            "bff_charges_filters_services_get_response.json",
        ).forEach { Contratos.decode(MovementsResponse.serializer(), "charges", it) }
        assertNull(cobros.filterScreen?.let { null })
        assertTrue(withFilters.filterScreen != null)
        assertEquals(listOf("app", "payment_link", "approved", "today"), withFilters.activeFilters!!.map { it.id })
    }

    @Test
    fun `la primera pagina se pide con la pestana, la pagina y el tamano`() = runTest(dispatcher) {
        val useCase = FakeMovements(cobros)
        val vm = viewModel(useCase)
        vm.start()
        advanceUntilIdle()
        val query = useCase.queries.single()
        assertEquals(1, query.page)
        assertEquals(3, query.size)
        assertEquals("charges", query.type)
        assertEquals("charges", query.queryItems()["history-tab"])
        assertEquals(cobros.charges, vm.loaded().charges)
        assertEquals(listOf("charges", "services"), vm.loaded().tabs.map { it.id })
        assertEquals(0, vm.loaded().currentTabIndex)
    }

    @Test
    fun `si falla la primera pagina la pantalla falla`() = runTest(dispatcher) {
        val vm = viewModel(FakeMovements(cobros).apply { fail = true })
        vm.start()
        advanceUntilIdle()
        assertEquals(MovementsUiState.Failed, vm.state.value)
    }

    @Test
    fun `una pagina completa habilita pedir mas y una incompleta no`() = runTest(dispatcher) {
        val full = FakeMovements(cobros).apply { pages = { listOf(charge(1), charge(2), charge(3)) } }
        val a = viewModel(full)
        a.start(); advanceUntilIdle()
        assertTrue(a.loaded().hasMoreMovements)

        val partial = FakeMovements(cobros).apply { pages = { listOf(charge(1)) } }
        val b = viewModel(partial)
        b.start(); advanceUntilIdle()
        assertFalse(b.loaded().hasMoreMovements)
    }

    @Test
    fun `la paginacion agrega cobros y corta con una pagina vacia`() = runTest(dispatcher) {
        val useCase = FakeMovements(cobros).apply {
            pages = { query -> if (query.page == 1) List(3) { charge(it) } else if (query.page == 2) List(3) { charge(10 + it) } else emptyList() }
        }
        val vm = viewModel(useCase)
        vm.start(); advanceUntilIdle()

        vm.loadNextPage(); advanceUntilIdle()
        assertEquals(6, vm.loaded().charges.size)
        assertEquals(2, vm.loaded().page)
        assertTrue(vm.loaded().hasMoreMovements)

        vm.loadNextPage(); advanceUntilIdle()
        assertEquals(6, vm.loaded().charges.size)
        assertFalse(vm.loaded().hasMoreMovements)

        vm.loadNextPage(); advanceUntilIdle()
        assertEquals(3, useCase.queries.size, "no vuelve a pedir cuando ya no hay más")
    }

    @Test
    fun `no pide otra pagina mientras carga la anterior`() = runTest(dispatcher) {
        val useCase = FakeMovements(cobros).apply { pages = { List(3) { n -> charge(n) } } }
        val vm = viewModel(useCase)
        vm.start(); advanceUntilIdle()
        vm.loadNextPage()
        vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(2, useCase.queries.size)
    }

    @Test
    fun `cambiar de pestana consulta la nueva sin los filtros aplicados`() = runTest(dispatcher) {
        val session = MovementsSession(3).apply { appliedFilters = MovementsQuery(page = 1, channel = "app", status = "approved") }
        val useCase = FakeMovements(withFilters)
        val vm = viewModel(useCase, session)
        vm.start(); advanceUntilIdle()
        // La primera carga sí lleva los filtros
        assertEquals("app", useCase.queries.first().channel)

        vm.didSelectTab(1)
        assertTrue(vm.loaded().isFilterLoading)
        assertTrue(vm.loaded().charges.isEmpty())
        advanceUntilIdle()
        val query = useCase.queries.last()
        assertEquals("services", query.type)
        assertNull(query.channel)
        assertEquals("services", session.selectedTabId)
        assertEquals(1, vm.loaded().currentTabIndex)
        assertFalse(vm.loaded().isFilterLoading)
    }

    @Test
    fun `elegir la misma pestana o una deshabilitada no hace nada`() = runTest(dispatcher) {
        val disabled = cobros.copy(tabsOptions = cobros.tabsOptions!!.map { if (it.id == "services") it.copy(disabled = true) else it })
        val useCase = FakeMovements(disabled)
        val vm = viewModel(useCase)
        vm.start(); advanceUntilIdle()
        vm.didSelectTab(0)
        vm.didSelectTab(1)
        advanceUntilIdle()
        assertEquals(1, useCase.queries.size)
    }

    @Test
    fun `sin pestanas del BFF se arma una sola segun el tipo`() = runTest(dispatcher) {
        val vm = viewModel(FakeMovements(cobros.copy(tabsOptions = null)))
        vm.start(); advanceUntilIdle()
        assertEquals(listOf("Cobros"), vm.loaded().tabs.map { it.title })
    }

    @Test
    fun `limpiar filtros recarga sin filtros`() = runTest(dispatcher) {
        val session = MovementsSession(3).apply { appliedFilters = MovementsQuery(page = 1, channel = "app") }
        val useCase = FakeMovements(withFilters)
        val vm = viewModel(useCase, session)
        vm.start(); advanceUntilIdle()
        assertTrue(vm.loaded().isFilterActive)

        vm.clearFilters()
        assertTrue(vm.loaded().isRefreshing)
        advanceUntilIdle()
        assertNull(session.appliedFilters)
        assertNull(useCase.queries.last().channel)
        assertFalse(vm.loaded().isRefreshing)
    }

    @Test
    fun `aplicar filtros desde la sesion recarga el historial`() = runTest(dispatcher) {
        val session = MovementsSession(3)
        val useCase = FakeMovements(cobros)
        val vm = viewModel(useCase, session)
        vm.start(); advanceUntilIdle()
        session.appliedFilters = MovementsQuery(page = 1, status = "approved")
        session.notifyFiltersApplied()
        advanceUntilIdle()
        assertEquals(2, useCase.queries.size)
        assertEquals("approved", useCase.queries.last().status)
    }

    @Test
    fun `el pull to refresh conserva la lista si falla`() = runTest(dispatcher) {
        val useCase = FakeMovements(cobros)
        val vm = viewModel(useCase)
        vm.start(); advanceUntilIdle()
        useCase.fail = true
        vm.refresh(); advanceUntilIdle()
        assertEquals(cobros.charges, vm.loaded().charges)
        assertFalse(vm.loaded().isRefreshing)
    }

    @Test
    fun `tocar un cobro abre su detalle con el canal filtrado`() = runTest(dispatcher) {
        val session = MovementsSession(3).apply { appliedFilters = MovementsQuery(page = 1, channel = "upos") }
        val vm = viewModel(FakeMovements(cobros), session)
        vm.start(); advanceUntilIdle()
        val charge = vm.loaded().charges.first()
        vm.didTapCharge(charge)
        assertEquals(
            MovementsDestination.Push(CIARoute.Detail(ticketNumber = charge.ticketNumber, channel = "upos")),
            vm.destinations.first(),
        )
    }

    @Test
    fun `el boton de filtros abre el formulario del BFF`() = runTest(dispatcher) {
        val vm = viewModel(FakeMovements(withFilters))
        vm.start(); advanceUntilIdle()
        vm.didTapFilter()
        val destination = vm.destinations.first()
        assertIs<MovementsDestination.Push>(destination)
        assertIs<CIARoute.MovementsFilters>(destination.route)
    }

    // region Filtros

    private val filterScreen = withFilters.filterScreen!!

    @Test
    fun `el formulario arranca con el primer canal y los filtros aplicados`() {
        val fresh = MovementsFiltersViewModel(filterScreen, MovementsSession(3))
        assertEquals(filterScreen.channel.options.first().id, fresh.state.value.selectedChannel)

        val session = MovementsSession(3).apply {
            appliedFilters = MovementsQuery(page = 1, channel = "upos", method = "qr", amountFrom = 5000.0)
        }
        val applied = MovementsFiltersViewModel(filterScreen, session).state.value
        assertEquals("upos", applied.selectedChannel)
        assertEquals("qr", applied.selectedMethod)
        assertEquals(5000.0, applied.amountFrom)
    }

    @Test
    fun `los metodos dependen del canal y cambiar de canal descarta el metodo`() {
        val vm = MovementsFiltersViewModel(filterScreen, MovementsSession(3))
        vm.selectMethod("qr")
        assertEquals("qr", vm.state.value.selectedMethod)
        vm.selectChannel("upos")
        assertNull(vm.state.value.selectedMethod)
        val allowed = vm.state.value.methodOptions
        assertTrue(allowed.all { it.channel == null || "upos" in it.channel!! })
    }

    @Test
    fun `sin canal el metodo queda deshabilitado`() {
        val vm = MovementsFiltersViewModel(filterScreen, MovementsSession(3))
        vm.selectChannel(null)
        assertFalse(vm.state.value.methodEnabled)
    }

    @Test
    fun `aplicar guarda los filtros en la sesion, avisa y vuelve`() = runTest(dispatcher) {
        val session = MovementsSession(30).apply { selectedTabId = "services" }
        var notified = 0
        val collector = launchCollect(session) { notified++ }
        // El colector tiene que estar suscripto antes de emitir (el flujo no guarda emisiones sin suscriptores)
        advanceUntilIdle()
        val vm = MovementsFiltersViewModel(filterScreen, session)
        vm.selectChannel("app"); vm.selectStatus("approved"); vm.setPeriodFrom("01/10/24"); vm.setAmountTo(9000.0)
        vm.applyFilters()
        val applied = session.appliedFilters!!
        assertEquals("services", applied.type)
        assertEquals(30, applied.size)
        assertEquals("app", applied.channel)
        assertEquals("approved", applied.status)
        assertEquals("01/10/24", applied.periodFrom)
        assertEquals("9000.0", applied.queryItems()["amountTo"])
        assertEquals(MovementsFiltersDestination.Pop, vm.destinations.first())
        advanceUntilIdle()
        assertEquals(1, notified)
        collector.cancel()
    }

    @Test
    fun `limpiar deja el formulario vacio`() {
        val vm = MovementsFiltersViewModel(filterScreen, MovementsSession(3))
        vm.selectStatus("approved"); vm.setPeriodTo("10/10/24")
        vm.clearFilters()
        val state = vm.state.value
        assertNull(state.selectedChannel)
        assertNull(state.selectedStatus)
        assertNull(state.periodTo)
    }

    @Test
    fun `las mascaras de fecha y de miles`() {
        assertEquals("12/03/24", MovementsFiltersViewModel.dateMask("120324"))
        assertEquals("12/03", MovementsFiltersViewModel.dateMask("1203"))
        assertEquals("1", MovementsFiltersViewModel.dateMask("1x"))
        assertEquals("12/03/24", MovementsFiltersViewModel.dateMask("12/03/2499"))
        assertEquals("1.250.000", MovementsFiltersViewModel.thousands("1250000"))
        assertEquals("999", MovementsFiltersViewModel.thousands("999"))
        assertEquals(1250000.0, MovementsFiltersViewModel.amountOf("1.250.000"))
        assertNull(MovementsFiltersViewModel.amountOf(""))
    }

    private fun kotlinx.coroutines.test.TestScope.launchCollect(session: MovementsSession, onEach: () -> Unit) =
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) { session.didApplyFilters.collect { onEach() } }

    // endregion
}
