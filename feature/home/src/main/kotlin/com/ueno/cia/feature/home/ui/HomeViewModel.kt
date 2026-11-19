package com.ueno.cia.feature.home.ui

import com.ueno.cia.core.analytics.HomeAnalytics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.model.MovementChargeModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.ProductType
import com.ueno.cia.core.navigation.ProfileOrigin
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.core.tenant.TenantContext
import com.ueno.cia.feature.home.data.HomeBannerModel
import com.ueno.cia.feature.home.data.HomeBranchModel
import com.ueno.cia.feature.home.data.HomeHeaderResponse
import com.ueno.cia.core.model.HomeInformationAlertModel
import com.ueno.cia.feature.home.data.HomeLayoutModel
import com.ueno.cia.feature.home.data.HomeLayoutSectionModel
import com.ueno.cia.core.model.HomeOptionKind
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.feature.home.data.HomeOptionsResponse
import com.ueno.cia.feature.home.data.HomeSectionID
import com.ueno.cia.feature.home.domain.HomeUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Equivale a `HomeNotifier` + la navegación de `HomeScreen` (home_notifier.dart / home_screen.dart). */
class HomeViewModel(
    private val useCase: HomeUseCase,
    private val tenantContext: TenantContext,
    private val analytics: HomeAnalytics,
) : ViewModel() {

    // region Outputs

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _destinations = Channel<HomeDestination>(Channel.BUFFERED)
    val destinations: Flow<HomeDestination> = _destinations.receiveAsFlow()

    // endregion

    /** Padre de todo lo que está en vuelo: se cancela en cada recarga completa. */
    private val loadJob: Job = SupervisorJob(viewModelScope.coroutineContext[Job])
    private val loadScope = CoroutineScope(viewModelScope.coroutineContext + loadJob)

    private val loadedState: HomeState? get() = (_state.value as? HomeUiState.Loaded)?.state

    override fun onCleared() {
        // Flutter: ref.onDispose(() => _setActiveBranchId(null))
        tenantContext.setActiveBranchId(null)
        loadJob.cancel()
    }

    // region Carga

    private var started = false

    /** Primera carga de la pantalla: sobrevive a rotaciones (el ViewModel no se recrea) sin volver a pedir todo. */
    fun start() {
        if (started) return
        started = true
        load()
    }

    /** `build()`: layout primero, después las secciones presentes en paralelo. */
    fun load() {
        loadJob.cancelChildren()
        _state.value = HomeUiState.Loading
        loadScope.launch { fetchLayout() }
    }

    /** Pull to refresh (`refreshVisibleSections` → `homeRetry`). */
    fun refresh() {
        val current = loadedState ?: return load()
        loadJob.cancelChildren()
        _isRefreshing.value = true
        // Mantiene la estructura y pone cada sección en skeleton mientras recarga
        _state.value = HomeUiState.Loaded(HomeState(current.appBarTitle, current.sections))
        loadScope.launch { fetchLayout() }
    }

    private suspend fun fetchLayout() {
        val response = attempt { useCase.layout() }.getOrElse {
            _isRefreshing.value = false
            _state.value = HomeUiState.Failed
            return
        }
        handleLayout(response.layout)
    }

    private suspend fun handleLayout(layout: HomeLayoutModel) {
        tenantContext.setActiveBranchId(null)
        val errorScreen = layout.errorScreen
        if (errorScreen != null) {
            _isRefreshing.value = false
            _state.value = HomeUiState.Failed
            _destinations.send(HomeDestination.Reset(CIARoute.Congrats(CongratsInput(errorScreen, CongratsOrigin.HOME_LAYOUT))))
            return
        }
        val state = HomeState(layout.appBarTitle, layout.sections)
        _state.value = HomeUiState.Loaded(state)
        loadSections(state)
    }

    /** `_buildSectionLoadFutures`: solo las secciones del layout; recents (endpoint) siempre. */
    private suspend fun loadSections(state: HomeState) {
        coroutineScope {
            if (state.hasHeader) launch { loadHeader() }
            if (state.has(HomeSectionID.Options)) launch { loadOptions() }
            if (state.has(HomeSectionID.Alerts)) launch { loadAlerts() }
            if (state.hasPendingSettlement) launch { loadPendingSettlement() }
            if (state.has(HomeSectionID.Banners)) launch { loadBanners() }
            if (state.has(HomeSectionID.Recents)) launch { loadCharges() }
            launch { loadRecents() }
        }
        _isRefreshing.value = false
    }

    private suspend fun loadHeader() = section(
        fetch = { useCase.header() },
        onSuccess = { state, response -> state.copy(header = Loadable.Loaded(headerContent(response))) },
        onFailure = { it.copy(header = Loadable.Failed) },
    )

    private suspend fun loadOptions() = section(
        fetch = { useCase.options() },
        onSuccess = { state, response -> state.copy(options = Loadable.Loaded(optionsContent(response))) },
        onFailure = { it.copy(options = Loadable.Failed) },
    )

    private suspend fun loadAlerts() = section(
        fetch = { useCase.alerts() },
        onSuccess = { state, response -> state.copy(alerts = Loadable.Loaded(response.alerts)) },
        onFailure = { it.copy(alerts = Loadable.Failed) },
    )

    private suspend fun loadPendingSettlement() = section(
        fetch = { useCase.pendingSettlement().value },
        onSuccess = { state, value -> state.copy(pendingSettlement = Loadable.Loaded(value)) },
        onFailure = { it.copy(pendingSettlement = Loadable.Failed) },
    )

    private suspend fun loadBanners() = section(
        fetch = { useCase.banners() },
        onSuccess = { state, response -> state.copy(banners = Loadable.Loaded(response.banners)) },
        onFailure = { it.copy(banners = Loadable.Failed) },
    )

    private suspend fun loadCharges() = section(
        fetch = { useCase.charges(page = 1) },
        onSuccess = { state, response -> state.copy(charges = Loadable.Loaded(response.charges)) },
        onFailure = { it.copy(charges = Loadable.Failed) },
    )

    private suspend fun loadRecents() = section(
        fetch = { useCase.recents() },
        onSuccess = { state, response -> state.copy(recents = Loadable.Loaded(response)) },
        onFailure = { it.copy(recents = Loadable.Failed) },
    )

    /** Cada sección aplica solo su parte sobre el estado vigente (Flutter re-captura `state.value` tras el await). */
    private suspend fun <T> section(
        fetch: suspend () -> T,
        onSuccess: (HomeState, T) -> HomeState,
        onFailure: (HomeState) -> HomeState,
    ) {
        attempt { fetch() }.fold(
            onSuccess = { value -> update { onSuccess(it, value) } },
            onFailure = { update(onFailure) },
        )
    }

    private fun update(transform: (HomeState) -> HomeState) {
        _state.update { current ->
            if (current is HomeUiState.Loaded) HomeUiState.Loaded(transform(current.state)) else current
        }
    }

    // endregion

    // region Reintentos por sección

    fun retryHeader() = retry({ it.copy(header = Loadable.Loading) }) { loadHeader() }
    fun retryOptions() = retry({ it.copy(options = Loadable.Loading) }) { loadOptions() }
    fun retryAlerts() = retry({ it.copy(alerts = Loadable.Loading) }) { loadAlerts() }
    fun retryBanners() = retry({ it.copy(banners = Loadable.Loading) }) { loadBanners() }
    fun retryPendingSettlement() = retry({ it.copy(pendingSettlement = Loadable.Loading) }) { loadPendingSettlement() }

    /** `chargesRetry` (+ recents si también había fallado). */
    fun retryRecents() {
        val state = loadedState ?: return
        update { it.copy(charges = Loadable.Loading) }
        loadScope.launch { loadCharges() }
        if (state.recents == Loadable.Failed) {
            update { it.copy(recents = Loadable.Loading) }
            loadScope.launch { loadRecents() }
        }
    }

    private fun retry(toLoading: (HomeState) -> HomeState, load: suspend () -> Unit) {
        update(toLoading)
        loadScope.launch { load() }
    }

    // endregion

    // region Sucursales

    fun didSwipeBranch(branch: HomeBranchModel) {
        tenantContext.setActiveBranchId(branch.branchId)
        update { it.copy(selectedBranchId = branch.branchId) }
        reloadBranchSections()
    }

    fun didSelectBranch(branch: HomeBranchModel) {
        update { it.copy(pinnedBranchId = branch.branchId) }
        didSwipeBranch(branch)
    }

    /** Página "Mostrar todas las sucursales" del carrusel (`enterBranchSelection`). */
    fun didReachShowAllBranchesPage() {
        update { it.copy(isBranchSelectionActive = true, pendingSettlement = Loadable.Loading) }
    }

    fun didTapShowAllBranches() {
        val branches = loadedState?.header?.valueOrNull?.branchCarousel?.branches ?: return
        emit(HomeDestination.BranchSheet(branches))
    }

    /** `reloadBranchSections`: recarga lo que depende de la sucursal. */
    private fun reloadBranchSections() {
        val state = loadedState ?: return
        loadJob.cancelChildren()
        update { current ->
            current.copy(
                isBranchSelectionActive = false,
                options = if (state.has(HomeSectionID.Options)) Loadable.Loading else current.options,
                banners = if (state.has(HomeSectionID.Banners)) Loadable.Loading else current.banners,
                charges = if (state.has(HomeSectionID.Recents)) Loadable.Loading else current.charges,
                alerts = if (state.has(HomeSectionID.Alerts)) Loadable.Loading else current.alerts,
                pendingSettlement = if (state.hasPendingSettlement) Loadable.Loading else current.pendingSettlement,
                recents = Loadable.Loading,
            )
        }
        loadScope.launch {
            coroutineScope {
                if (state.has(HomeSectionID.Options)) launch { loadOptions() }
                if (state.hasPendingSettlement) launch { loadPendingSettlement() }
                if (state.has(HomeSectionID.Banners)) launch { loadBanners() }
                if (state.has(HomeSectionID.Recents)) launch { loadCharges() }
                launch { loadRecents() }
                if (state.has(HomeSectionID.Alerts)) launch { loadAlerts() }
            }
        }
    }

    // endregion

    // region Acciones del usuario

    fun didTapBack() = emit(HomeDestination.Exit)

    fun didTapProfile() {
        val button = loadedState?.header?.valueOrNull?.profileButton ?: return
        analytics.editProfileTap(button.text)
        // `isLegacyProfile`: con ese deeplink (y fuera de Empresas) el botón edita el nombre directo, sin pasar por el Perfil
        val isLegacyProfile = button.deeplink == ProfileOrigin.PROFILE_NAME.raw
        if (tenantContext.tenant != Tenant.EMPRESAS && isLegacyProfile) {
            emit(HomeDestination.Push(CIARoute.ProfileName(ProfileOrigin.HOME)))
        } else {
            emit(HomeDestination.Push(CIARoute.Profile))
        }
    }

    fun didTapHelp() {
        val button = loadedState?.header?.valueOrNull?.helpButton ?: return
        analytics.helpTap(button.text)
        emit(HomeDestination.HelpCenter(button.deeplink.orEmpty()))
    }

    /** Opción de la grilla: si trae modal abre el bottom sheet, si no navega (`_initCharge`). */
    fun didTapOption(option: HomeOptionModel) {
        if (option.disabled) return
        val modal = option.modal
        if (modal != null) emit(HomeDestination.OptionsSheet(modal)) else goToInitCharge(option)
    }

    /** Opción dentro del bottom sheet. */
    fun didSelectModalOption(option: HomeOptionModel) {
        if (option.disabled) return
        goToInitCharge(option)
    }

    fun didTapBanner(banner: HomeBannerModel) {
        if (banner.actionType == "products_onboarding") {
            when (banner.type) {
                ProductType.LINK -> analytics.linkOnboardingStart(banner.buttonText)
                ProductType.SUBSCRIPTION -> analytics.subscriptionOnboardingStart(banner.buttonText)
            }
        }
        // `products_onboarding`: el tipo de producto lo define el banner (`productsOnboardingFlow.init(productType:)`)
        if (banner.actionType == "products_onboarding" &&
            (banner.type == ProductType.LINK || banner.type == ProductType.SUBSCRIPTION)
        ) {
            return emit(HomeDestination.Push(CIARoute.ProductsOnboardingEntryPoint(banner.type)))
        }
        emit(HomeDestination.Push(CIARoute.Deeplink(banner.actionType)))
    }

    /** `homeRucValidation`. */
    fun didTapAlertAction(alert: HomeInformationAlertModel) {
        val link = alert.actionLink ?: return
        when (link.action) {
            "UPDATE_RUC" -> {
                analytics.validateRucStart(link.text)
                emit(HomeDestination.Push(CIARoute.TaxInfo))
            }
            "UPDATE_FIELD" -> {
                analytics.updateFieldStart(link.text)
                emit(HomeDestination.Push(CIARoute.UpdateField))
            }
        }
    }

    fun didTapShowAllMovements() {
        val state = loadedState ?: return
        val history = state.recents.valueOrNull?.historyScreen ?: return
        analytics.movementsTap(state.recentsLink?.text.orEmpty())
        emit(HomeDestination.Push(CIARoute.Movements(history)))
    }

    fun didTapMovement(charge: MovementChargeModel) {
        emit(HomeDestination.Push(CIARoute.Detail(ticketNumber = charge.ticketNumber)))
    }

    // endregion

    // region Navegación de opciones (`_goToInitCharge` + `goToInitCharge`)

    private fun goToInitCharge(option: HomeOptionModel) {
        when (option.id) {
            "LINK_LIST" -> {
                analytics.myLinksTap(option.title)
                return emit(HomeDestination.Push(CIARoute.LinkList))
            }
            "QR_EMPRESAS" -> return emit(HomeDestination.Push(businessSelector(type = "QR")))
            "LINK_EMPRESAS" -> return emit(HomeDestination.Push(businessSelector(type = "LINK")))
            "MOVEMENTS_EMPRESAS" -> return didTapShowAllMovements()
        }

        val label = option.title
        when (option.kind) {
            HomeOptionKind.QrUeno -> startQr(ChargeProvider.UPAY, label)
            HomeOptionKind.QrBancard -> startQr(ChargeProvider.BANCARD, label)
            HomeOptionKind.QrDepay -> startQr(ChargeProvider.DEPAY, label)
            HomeOptionKind.QrPix -> startQr(ChargeProvider.PIX, label)
            HomeOptionKind.QrSip -> startQr(ChargeProvider.SIP, label)
            HomeOptionKind.QrBolivia -> startQr(ChargeProvider.BOLIVIA, label)
            HomeOptionKind.QrPeru -> startQr(ChargeProvider.PERU, label)
            HomeOptionKind.QrColombia -> startQr(ChargeProvider.COLOMBIA, label)
            HomeOptionKind.QrInter -> {
                analytics.qrStart(label, ChargeProvider.INTER)
                emit(HomeDestination.Push(CIARoute.PaymentSelectorCountry()))
            }
            HomeOptionKind.QrInternacional -> emit(HomeDestination.Push(CIARoute.PaymentSelectorCountry()))
            HomeOptionKind.Link -> {
                analytics.linkStart(label)
                emit(HomeDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.LINK, ChargeProvider.LINK)))
            }
            HomeOptionKind.LinkList -> {
                analytics.myLinksTap(label)
                emit(HomeDestination.Push(CIARoute.LinkList))
            }
            // Tap to Phone es solo Android: la feature todavía no está migrada (placeholder en el host)
            HomeOptionKind.Ttp -> emit(HomeDestination.Push(CIARoute.TapToPhone))
        }
    }

    /** `setXType(type, branchId:, xOrigin: 'SELECTOR_BUSINESS')`. */
    private fun businessSelector(type: String): CIARoute =
        CIARoute.PaymentSelectorEntryPoint(type = type, branchId = loadedState?.selectedBranchId, origin = "SELECTOR_BUSINESS")

    private fun startQr(provider: ChargeProvider, label: String) {
        analytics.qrStart(label, provider)
        emit(HomeDestination.Push(CIARoute.ValidateLimitEntryPoint(ChargeType.QR, provider)))
    }

    // endregion

    private fun emit(destination: HomeDestination) {
        _destinations.trySend(destination)
    }

    companion object {
        private fun headerContent(response: HomeHeaderResponse) = HomeHeaderContent(
            merchantName = response.individuos?.merchantName.orEmpty(),
            subscriptionPlan = response.subscriptionPlan,
            profileButton = response.editProfileButton,
            helpButton = response.helpButton,
            branchCarousel = if (response.isBranchCarousel) {
                response.branchCarouselData ?: HomeHeaderResponse.BranchCarousel(emptyList(), null)
            } else {
                null
            },
        )

        /** `_getOptions` + `_getMessage`. Android muestra TTP (en iOS se filtra). */
        fun optionsContent(response: HomeOptionsResponse): HomeOptionsContent {
            val options = response.options
            val message = response.messages.firstOrNull { it.isUserInBlacklist }
                ?: if (options.isEmpty()) response.messages.firstOrNull { it.isEmptyOptions } else null
            return HomeOptionsContent(options, message)
        }

        /** Como `runCatching`, pero la cancelación de la corrutina se propaga. */
        private inline fun <T> attempt(block: () -> T): Result<T> =
            try {
                Result.success(block())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
    }
}
