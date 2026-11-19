package com.ueno.cia.feature.link.domain

import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.feature.link.data.LinkGenerateRequest
import com.ueno.cia.feature.link.data.LinkListResponse

interface LinkGenerateUseCase {
    suspend fun generate(request: LinkGenerateRequest): CongratsConfirmResponse
}

interface LinkListUseCase {
    suspend fun list(status: String, page: Int, size: Int): LinkListResponse
}
