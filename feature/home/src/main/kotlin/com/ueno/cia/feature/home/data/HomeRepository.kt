package com.ueno.cia.feature.home.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.home.domain.HomeUseCase

/** Endpoints y tipos de evento iguales a los repositorios Flutter (`home_*_repository_impl.dart`). */
class HomeRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : HomeUseCase {
    override suspend fun layout(): HomeLayoutResponse =
        get(CIAEndpoints.Home.LAYOUT, "Home - Get Home Layout")

    override suspend fun header(): HomeHeaderResponse =
        get(CIAEndpoints.Home.HEADER, "Home - Get Home Header")

    override suspend fun options(): HomeOptionsResponse =
        get(CIAEndpoints.Home.OPTIONS, "Home - Get Home Options")

    override suspend fun banners(): HomeBannersResponse =
        get(CIAEndpoints.Home.BANNERS, "Home - Get Home Banners")

    override suspend fun recents(): HomeRecentsResponse =
        get(CIAEndpoints.Home.RECENTS, "Home - Get Home Recents")

    override suspend fun alerts(): HomeAlertsResponse =
        get(CIAEndpoints.Home.ALERTS, "Home - Get Home Alerts")

    override suspend fun pendingSettlement(): HomePendingSettlementResponse =
        get(CIAEndpoints.Home.PENDING_SETTLEMENT, "Home - Get Home Pending Settlement")

    override suspend fun charges(page: Int): MovementsResponse =
        get(CIAEndpoints.Home.CHARGES, "Get Movements", query = mapOf("page" to page.toString()))

    private suspend inline fun <reified Response> get(
        path: String,
        eventType: String,
        query: Map<String, String> = emptyMap(),
    ): Response = events.reportingError(eventType, path) { httpClient.get<Response>(path, query) }
}
