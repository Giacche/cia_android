package com.ueno.cia.feature.validatelimit.domain

import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.feature.validatelimit.data.ValidateLimitResponse

interface ValidateLimitUseCase {
    suspend fun validateLimit(chargeType: ChargeType, provider: ChargeProvider): ValidateLimitResponse
}
