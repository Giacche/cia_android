package com.ueno.cia.kit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.analytics.NoOpAnalyticsTracking
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.NoOpEvents
import com.ueno.cia.core.flags.FeatureFlags
import com.ueno.cia.core.flags.InMemoryFeatureFlags
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.core.network.HttpClient
import com.ueno.cia.core.platform.AlwaysConnected
import com.ueno.cia.core.platform.ConnectivityChecking
import com.ueno.cia.core.tenant.TenantContext
import com.ueno.cia.feature.detail.data.DetailRepository
import com.ueno.cia.feature.detail.domain.DetailUseCase
import com.ueno.cia.feature.home.data.HomeRepository
import com.ueno.cia.feature.home.domain.HomeUseCase
import com.ueno.cia.feature.link.data.LinkRepository
import com.ueno.cia.feature.movements.data.MovementsRepository
import com.ueno.cia.feature.movements.domain.MovementsUseCase
import com.ueno.cia.feature.paymentselector.data.PaymentSelectorRepository
import com.ueno.cia.feature.paymentselector.domain.PaymentSelectorUseCase
import com.ueno.cia.feature.qr.data.QRGenerateRepository
import com.ueno.cia.feature.qr.domain.QRGenerateUseCase
import com.ueno.cia.feature.validatelimit.data.ValidateLimitRepository
import com.ueno.cia.feature.validatelimit.domain.ValidateLimitUseCase

/** Lo que el host le provee a CIA (equivale a las dependencias que inyecta la superapp en Flutter / iOS). */
class CIADependencies(
    val httpClient: HttpClient,
    val tenantContext: TenantContext,
    val analyticsTracking: AnalyticsTracking = NoOpAnalyticsTracking,
    val events: EventsReporting = NoOpEvents,
    /** El polling de QR solo consulta la confirmación si hay red (`connectivity_plus` en Flutter). */
    val connectivity: ConnectivityChecking = AlwaysConnected,
    /** Feature flags del host (Remote Config, un servicio propio o DataStore). Por defecto, todo con su valor por defecto. */
    val featureFlags: FeatureFlags = InMemoryFeatureFlags(),
)

/** Lo que CIA le pide al host cuando la navegación sale del módulo. */
interface CIAHost {
    /** El usuario salió de CIA (atrás desde la Home). */
    fun onExit()

    /** Abrir el Centro de ayuda con el deeplink del BFF. */
    fun onOpenHelpCenter(deeplink: String)
}

/** Contenedor de dependencias de CIA: construye los repositorios sobre el cliente HTTP del host. */
internal class CIAContainer(val dependencies: CIADependencies) {
    val httpClient = CIAHttpClient(dependencies.httpClient, dependencies.tenantContext)
    val analytics = Analytics(dependencies.analyticsTracking)
    val featureFlags: FeatureFlags = dependencies.featureFlags
    val homeUseCase: HomeUseCase = HomeRepository(httpClient, dependencies.events)
    val validateLimitUseCase: ValidateLimitUseCase = ValidateLimitRepository(httpClient, dependencies.events)
    val qrGenerateUseCase: QRGenerateUseCase = QRGenerateRepository(httpClient, dependencies.events)
    val movementsUseCase: MovementsUseCase = MovementsRepository(httpClient, dependencies.events)
    val detailUseCase: DetailUseCase = DetailRepository(httpClient, dependencies.events)
    val linkRepository = LinkRepository(httpClient, dependencies.events)
    val paymentSelectorUseCase: PaymentSelectorUseCase = PaymentSelectorRepository(httpClient, dependencies.events)
}

/**
 * Punto de entrada de CIA para la app host (`CIAModule` en iOS). La app `app` del repo es solo un sandbox sobre esto.
 *
 * ```
 * val cia = CIAModule(CIADependencies(httpClient, tenantContext))
 * cia.Content(host = myHost)
 * ```
 */
class CIAModule(private val dependencies: CIADependencies) {
    private val container = CIAContainer(dependencies)

    @Composable
    fun Content(host: CIAHost, modifier: Modifier = Modifier, startRoute: CIARoute = CIARoute.Home) {
        CIANavHost(container = container, host = host, startRoute = startRoute, modifier = modifier)
    }
}
