package com.ueno.cia.feature.home.domain

import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.feature.home.data.HomeAlertsResponse
import com.ueno.cia.feature.home.data.HomeBannersResponse
import com.ueno.cia.feature.home.data.HomeHeaderResponse
import com.ueno.cia.feature.home.data.HomeLayoutResponse
import com.ueno.cia.feature.home.data.HomeOptionsResponse
import com.ueno.cia.feature.home.data.HomePendingSettlementResponse
import com.ueno.cia.feature.home.data.HomeRecentsResponse

/** Contrato que consume `HomeViewModel` (en Flutter, un use case por sección). */
interface HomeUseCase {
    suspend fun layout(): HomeLayoutResponse
    suspend fun header(): HomeHeaderResponse
    suspend fun options(): HomeOptionsResponse
    suspend fun banners(): HomeBannersResponse
    suspend fun recents(): HomeRecentsResponse
    suspend fun alerts(): HomeAlertsResponse
    suspend fun pendingSettlement(): HomePendingSettlementResponse
    suspend fun charges(page: Int): MovementsResponse
}
