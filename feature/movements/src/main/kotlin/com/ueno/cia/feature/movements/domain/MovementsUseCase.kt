package com.ueno.cia.feature.movements.domain

import com.ueno.cia.core.model.MovementsQuery
import com.ueno.cia.core.model.MovementsResponse

interface MovementsUseCase {
    suspend fun getMoves(query: MovementsQuery): MovementsResponse
}
