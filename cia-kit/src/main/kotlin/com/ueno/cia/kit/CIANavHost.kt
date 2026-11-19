package com.ueno.cia.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.MovementsFilterScreenModel
import com.ueno.cia.core.model.QRTimeoutModel
import com.ueno.cia.core.model.ReceiptScreenModel
import com.ueno.cia.core.model.MovementsScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CPFInput
import com.ueno.cia.core.navigation.InputAmountInput
import com.ueno.cia.core.navigation.PaymentSelectorInput
import com.ueno.cia.core.navigation.LinkFlowInput
import com.ueno.cia.core.navigation.QRFlowInput
import com.ueno.cia.feature.congrats.ui.CongratsDestination
import com.ueno.cia.feature.congrats.ui.CongratsRoute
import com.ueno.cia.feature.congrats.ui.CongratsViewModel
import com.ueno.cia.feature.cpf.ui.CPFDestination
import com.ueno.cia.feature.cpf.ui.CPFRoute
import com.ueno.cia.feature.cpf.ui.CPFViewModel
import com.ueno.cia.feature.detail.receipt.ReceiptScreen
import com.ueno.cia.feature.detail.ui.DetailDestination
import com.ueno.cia.feature.detail.ui.DetailRoute
import com.ueno.cia.feature.detail.ui.DetailViewModel
import com.ueno.cia.core.analytics.HomeAnalytics
import com.ueno.cia.feature.home.ui.HomeDestination
import com.ueno.cia.feature.home.ui.HomeRoute
import com.ueno.cia.feature.home.ui.HomeViewModel
import com.ueno.cia.feature.inputamount.ui.InputAmountDestination
import com.ueno.cia.feature.inputamount.ui.InputAmountRoute
import com.ueno.cia.feature.inputamount.ui.InputAmountViewModel
import com.ueno.cia.feature.link.ui.LinkGenerateDestination
import com.ueno.cia.feature.link.ui.LinkGenerateRoute
import com.ueno.cia.feature.link.ui.LinkGenerateViewModel
import com.ueno.cia.feature.link.ui.LinkListDestination
import com.ueno.cia.feature.link.ui.LinkListRoute
import com.ueno.cia.feature.link.ui.LinkListViewModel
import com.ueno.cia.feature.link.ui.LinkPreConfirmDestination
import com.ueno.cia.feature.link.ui.LinkPreConfirmRoute
import com.ueno.cia.feature.link.ui.LinkPreConfirmViewModel
import com.ueno.cia.feature.movements.ui.MovementsDestination
import com.ueno.cia.feature.movements.ui.MovementsFiltersRoute
import com.ueno.cia.feature.movements.ui.MovementsFiltersViewModel
import com.ueno.cia.feature.movements.ui.MovementsRoute
import com.ueno.cia.feature.movements.ui.MovementsSession
import com.ueno.cia.feature.movements.ui.MovementsViewModel
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorChargeNavigator
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorCountryRoute
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorCountryViewModel
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorDestination
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorEntryPointRoute
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorEntryPointViewModel
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorRoute
import com.ueno.cia.feature.paymentselector.ui.PaymentSelectorViewModel
import com.ueno.cia.feature.qr.ui.QRGenerateDestination
import com.ueno.cia.feature.qr.ui.QRGenerateRoute
import com.ueno.cia.feature.qr.ui.QRGenerateViewModel
import com.ueno.cia.feature.qr.ui.QRTimeoutDestination
import com.ueno.cia.feature.qr.ui.QRTimeoutRoute
import com.ueno.cia.feature.qr.ui.QRTimeoutViewModel
import com.ueno.cia.feature.validatelimit.ui.ValidateLimitDestination
import com.ueno.cia.feature.validatelimit.ui.ValidateLimitRoute
import com.ueno.cia.feature.validatelimit.ui.ValidateLimitViewModel
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** Grafo de navegación de CIA (`CIARouter` + `ScreenFactory` en iOS, `go_router` en Flutter). */
@Composable
internal fun CIANavHost(
    container: CIAContainer,
    host: CIAHost,
    startRoute: CIARoute,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val onBack: () -> Unit = remember(navController) {
        { if (!navController.popBackStack()) host.onExit() }
    }

    val platform = rememberPlatformActions()
    /** `goBackToHome`: vuelve a la Home; si ya no está en el stack (tras un `reset`) la abre de nuevo. */
    val goHome: () -> Unit = remember(navController) {
        {
            if (!navController.popBackStack<CIARoute.Home>(inclusive = false)) {
                navController.navigate(CIARoute.Home) { popUpTo(navController.graph.id) { inclusive = true } }
            }
        }
    }
    /** `go` de Flutter: el stack queda solo con la pantalla nueva. */
    val reset: (CIARoute) -> Unit = remember(navController) {
        { route -> navController.navigate(route) { popUpTo(navController.graph.id) { inclusive = true } } }
    }

    NavHost(navController = navController, startDestination = startRoute, modifier = modifier) {
        composable<CIARoute.Home> {
            val viewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        HomeViewModel(
                            useCase = container.homeUseCase,
                            tenantContext = container.dependencies.tenantContext,
                            analytics = HomeAnalytics(container.analytics),
                        )
                    }
                },
            )
            HomeRoute(
                viewModel = viewModel,
                tenant = container.dependencies.tenantContext.tenant,
                onNavigate = { destination ->
                    when (destination) {
                        is HomeDestination.Push -> navController.navigate(destination.route)
                        is HomeDestination.Reset -> navController.navigate(destination.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                        HomeDestination.Exit -> host.onExit()
                        is HomeDestination.HelpCenter -> host.onOpenHelpCenter(destination.deeplink)
                        // Los sheets los resuelve HomeRoute
                        is HomeDestination.OptionsSheet, is HomeDestination.BranchSheet -> Unit
                    }
                },
            )
        }

        composable<CIARoute.ValidateLimitEntryPoint> { entry ->
            val route = entry.toRoute<CIARoute.ValidateLimitEntryPoint>()
            val viewModel: ValidateLimitViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ValidateLimitViewModel(
                            chargeType = route.chargeType,
                            provider = route.provider,
                            canPop = route.canPop,
                            useCase = container.validateLimitUseCase,
                        )
                    }
                },
            )
            ValidateLimitRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        // `pushReplacement`: esta pantalla no queda en el stack
                        is ValidateLimitDestination.Replace -> navController.navigate(destination.route) {
                            popUpTo<CIARoute.ValidateLimitEntryPoint> { inclusive = true }
                        }
                        ValidateLimitDestination.Back -> onBack()
                    }
                },
            )
        }

        composable<CIARoute.InputAmount>(typeMap = mapOf(typeOf<InputAmountInput>() to jsonNavType<InputAmountInput>())) { entry ->
            val route = entry.toRoute<CIARoute.InputAmount>()
            val viewModel: InputAmountViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { InputAmountViewModel(route.input, container.analytics) }
                },
            )
            InputAmountRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is InputAmountDestination.Push -> navController.navigate(destination.route)
                        InputAmountDestination.Back -> onBack()
                        // Flutter: sin `canPop` el back vuelve a la Home
                        InputAmountDestination.GoBackToHome -> if (!navController.popBackStack<CIARoute.Home>(inclusive = false)) {
                            navController.navigate(CIARoute.Home) { popUpTo(navController.graph.id) { inclusive = true } }
                        }
                    }
                },
            )
        }

        composable<CIARoute.Cpf>(typeMap = mapOf(typeOf<CPFInput>() to jsonNavType<CPFInput>())) { entry ->
            val route = entry.toRoute<CIARoute.Cpf>()
            val viewModel: CPFViewModel = viewModel(
                factory = viewModelFactory { initializer { CPFViewModel(route.input, container.analytics) } },
            )
            CPFRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is CPFDestination.Push -> navController.navigate(destination.route)
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.QrGenerate>(typeMap = mapOf(typeOf<QRFlowInput>() to jsonNavType<QRFlowInput>())) { entry ->
            val route = entry.toRoute<CIARoute.QrGenerate>()
            val viewModel: QRGenerateViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        QRGenerateViewModel(
                            input = route.input,
                            useCase = container.qrGenerateUseCase,
                            connectivity = container.dependencies.connectivity,
                            analytics = container.analytics,
                        )
                    }
                },
            )
            QRGenerateRoute(
                viewModel = viewModel,
                platform = platform,
                onDestination = { destination ->
                    when (destination) {
                        // `pushReplacement`: el QR no queda en el stack (timeout o error al generar)
                        is QRGenerateDestination.Replace -> navController.navigate(destination.route) {
                            popUpTo<CIARoute.QrGenerate> { inclusive = true }
                        }
                        is QRGenerateDestination.Reset -> reset(destination.route)
                        QRGenerateDestination.Back -> onBack()
                        // El sheet de proveedores lo resuelve QRGenerateRoute
                        is QRGenerateDestination.ProviderSheet -> Unit
                    }
                },
            )
        }

        composable<CIARoute.QrTimeout>(typeMap = mapOf(typeOf<QRTimeoutModel>() to jsonNavType<QRTimeoutModel>())) { entry ->
            val route = entry.toRoute<CIARoute.QrTimeout>()
            val viewModel: QRTimeoutViewModel = viewModel(
                factory = viewModelFactory { initializer { QRTimeoutViewModel(route.screen) } },
            )
            QRTimeoutRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        QRTimeoutDestination.Back -> onBack()
                        QRTimeoutDestination.GoHome -> goHome()
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.Congrats>(typeMap = mapOf(typeOf<CongratsInput>() to jsonNavType<CongratsInput>())) { entry ->
            val route = entry.toRoute<CIARoute.Congrats>()
            val viewModel: CongratsViewModel = viewModel(
                factory = viewModelFactory { initializer { CongratsViewModel(route.input, container.analytics) } },
            )
            CongratsRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is CongratsDestination.Push -> navController.navigate(destination.route)
                        is CongratsDestination.Reset -> reset(destination.route)
                        // `_popOrGoHome`: vuelve una pantalla si hay a dónde, si no a la Home
                        CongratsDestination.PopOrGoHome ->
                            if (navController.previousBackStackEntry != null) navController.popBackStack() else goHome()
                        CongratsDestination.GoHome -> goHome()
                        is CongratsDestination.OpenUrl -> platform.openUrl(destination.url)
                        is CongratsDestination.CallPhone -> platform.callPhone(destination.number)
                        is CongratsDestination.Share -> platform.shareText(destination.text)
                        is CongratsDestination.Copy -> platform.copy(destination.value)
                    }
                },
            )
        }

        composable<CIARoute.Receipt>(typeMap = mapOf(typeOf<ReceiptScreenModel>() to jsonNavType<ReceiptScreenModel>())) { entry ->
            ReceiptScreen(screen = entry.toRoute<CIARoute.Receipt>().screen, platform = platform, onBack = onBack)
        }

        // region Fase 2: historial, detalle, link y selector de cobros

        composable<CIARoute.Movements>(typeMap = mapOf(typeOf<MovementsScreenModel>() to jsonNavType<MovementsScreenModel>())) { entry ->
            val route = entry.toRoute<CIARoute.Movements>()
            // La sesión (pestaña elegida + filtros aplicados) se comparte con la pantalla de filtros vía esta entrada
            val session: MovementsSession = viewModel(factory = viewModelFactory { initializer { MovementsSession(route.screen.pageSize) } })
            val viewModel: MovementsViewModel = viewModel(
                factory = viewModelFactory { initializer { MovementsViewModel(route.screen, container.movementsUseCase, session) } },
            )
            MovementsRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is MovementsDestination.Push -> navController.navigate(destination.route)
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.MovementsFilters>(
            typeMap = mapOf(typeOf<MovementsFilterScreenModel>() to jsonNavType<MovementsFilterScreenModel>()),
        ) { entry ->
            val route = entry.toRoute<CIARoute.MovementsFilters>()
            val movements = remember(entry) { navController.getBackStackEntry<CIARoute.Movements>() }
            val session: MovementsSession = viewModel(
                viewModelStoreOwner = movements,
                factory = viewModelFactory {
                    initializer { MovementsSession(movements.toRoute<CIARoute.Movements>().screen.pageSize) }
                },
            )
            val viewModel: MovementsFiltersViewModel = viewModel(
                factory = viewModelFactory { initializer { MovementsFiltersViewModel(route.filterScreen, session) } },
            )
            MovementsFiltersRoute(viewModel = viewModel, onPop = onBack, onBack = onBack)
        }

        composable<CIARoute.Detail> { entry ->
            val route = entry.toRoute<CIARoute.Detail>()
            val viewModel: DetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { DetailViewModel(route.ticketNumber, route.channel, container.detailUseCase, container.analytics) }
                },
            )
            DetailRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is DetailDestination.Push -> navController.navigate(destination.route)
                        is DetailDestination.Copy -> platform.copy(destination.value, destination.message)
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.LinkPreConfirm>(typeMap = mapOf(typeOf<LinkFlowInput>() to jsonNavType<LinkFlowInput>())) { entry ->
            val route = entry.toRoute<CIARoute.LinkPreConfirm>()
            val viewModel: LinkPreConfirmViewModel = viewModel(
                factory = viewModelFactory { initializer { LinkPreConfirmViewModel(route.input) } },
            )
            LinkPreConfirmRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is LinkPreConfirmDestination.Push -> navController.navigate(destination.route)
                        is LinkPreConfirmDestination.OpenDocument -> platform.openUrl(destination.url)
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.LinkGenerate>(typeMap = mapOf(typeOf<LinkFlowInput>() to jsonNavType<LinkFlowInput>())) { entry ->
            val route = entry.toRoute<CIARoute.LinkGenerate>()
            val viewModel: LinkGenerateViewModel = viewModel(
                factory = viewModelFactory { initializer { LinkGenerateViewModel(route.input, container.linkRepository) } },
            )
            LinkGenerateRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is LinkGenerateDestination.Reset -> reset(destination.route)
                        LinkGenerateDestination.Back -> onBack()
                    }
                },
            )
        }

        composable<CIARoute.LinkList> {
            val viewModel: LinkListViewModel = viewModel(
                factory = viewModelFactory { initializer { LinkListViewModel(container.linkRepository, container.analytics) } },
            )
            LinkListRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is LinkListDestination.Push -> navController.navigate(destination.route)
                        LinkListDestination.GoBackToHome -> goHome()
                        is LinkListDestination.Share -> platform.shareText(destination.text)
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.PaymentSelectorEntryPoint> { entry ->
            val route = entry.toRoute<CIARoute.PaymentSelectorEntryPoint>()
            val viewModel: PaymentSelectorEntryPointViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        PaymentSelectorEntryPointViewModel(route.type, route.branchId, route.origin, container.paymentSelectorUseCase)
                    }
                },
            )
            PaymentSelectorEntryPointRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        // `pushReplacement`: el entry point no queda en el stack
                        is PaymentSelectorDestination.Replace -> navController.navigate(destination.route) {
                            popUpTo<CIARoute.PaymentSelectorEntryPoint> { inclusive = true }
                        }
                        PaymentSelectorDestination.Back -> onBack()
                        is PaymentSelectorDestination.Push -> navController.navigate(destination.route)
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.PaymentSelector>(typeMap = mapOf(typeOf<PaymentSelectorInput>() to jsonNavType<PaymentSelectorInput>())) { entry ->
            val route = entry.toRoute<CIARoute.PaymentSelector>()
            val viewModel: PaymentSelectorViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { PaymentSelectorViewModel(route.input, PaymentSelectorChargeNavigator(HomeAnalytics(container.analytics))) }
                },
            )
            PaymentSelectorRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is PaymentSelectorDestination.Push -> navController.navigate(destination.route)
                        else -> Unit
                    }
                },
                onBack = onBack,
            )
        }

        composable<CIARoute.PaymentSelectorCountry> { entry ->
            val route = entry.toRoute<CIARoute.PaymentSelectorCountry>()
            val viewModel: PaymentSelectorCountryViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        PaymentSelectorCountryViewModel(
                            route.branchId,
                            route.origin,
                            container.paymentSelectorUseCase,
                            PaymentSelectorChargeNavigator(HomeAnalytics(container.analytics)),
                        )
                    }
                },
            )
            PaymentSelectorCountryRoute(
                viewModel = viewModel,
                onDestination = { destination ->
                    when (destination) {
                        is PaymentSelectorDestination.Push -> navController.navigate(destination.route)
                        PaymentSelectorDestination.Back -> onBack()
                        is PaymentSelectorDestination.Replace -> navController.navigate(destination.route)
                    }
                },
                onBack = onBack,
            )
        }

        // endregion

        // Features todavía no migradas: placeholder "Próximamente" que muestra la ruta y sus argumentos.
        pending<CIARoute.Profile>(onBack)
        pending<CIARoute.ProfileName>(onBack)
        pending<CIARoute.TaxInfo>(onBack)
        pending<CIARoute.UpdateField>(onBack)
        pending<CIARoute.ProductsOnboardingEntryPoint>(onBack)
        pending<CIARoute.TapToPhone>(onBack)
        pending<CIARoute.Deeplink>(onBack)
    }
}

private inline fun <reified R : CIARoute> NavGraphBuilder.pending(
    noinline onBack: () -> Unit,
    typeMap: Map<KType, NavType<*>> = emptyMap(),
) {
    composable<R>(typeMap = typeMap) { entry ->
        PendingScreen(route = entry.toRoute<R>().toString(), onBack = onBack)
    }
}
