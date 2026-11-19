package com.ueno.cia.feature.detail.domain

import com.ueno.cia.feature.detail.data.DetailResponse

interface DetailUseCase {
    suspend fun getDetail(ticketNumber: String, channel: String?): DetailResponse
}
