package com.ueno.cia.feature.movements.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.model.MovementsQuery
import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.movements.domain.MovementsUseCase

/** `movements_repository.dart`. */
class MovementsRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : MovementsUseCase {
    override suspend fun getMoves(query: MovementsQuery): MovementsResponse =
        events.reportingError("Get Movements", CIAEndpoints.Home.CHARGES) {
            httpClient.get<MovementsResponse>(CIAEndpoints.Home.CHARGES, query.queryItems())
        }
}
