package com.ueno.cia.feature.validatelimit.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.validatelimit.domain.ValidateLimitUseCase

/** `validate_limit_repository.dart`: el BFF recibe tipo y proveedor en headers. */
class ValidateLimitRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : ValidateLimitUseCase {
    override suspend fun validateLimit(chargeType: ChargeType, provider: ChargeProvider): ValidateLimitResponse =
        events.reportingError("Get Validate Limit", CIAEndpoints.ValidateLimit.PATH) {
            httpClient.get<ValidateLimitResponse>(
                CIAEndpoints.ValidateLimit.PATH,
                headers = mapOf("x-type" to chargeType.name, "x-provider" to provider.raw),
            )
        }
}
