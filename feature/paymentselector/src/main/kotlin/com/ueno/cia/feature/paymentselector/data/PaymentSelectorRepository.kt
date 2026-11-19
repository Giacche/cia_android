package com.ueno.cia.feature.paymentselector.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.model.PaymentSelectorResponse
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.paymentselector.domain.PaymentSelectorUseCase

/** `payment_selector_entry_point_repository.dart`: tipo, sucursal y origen viajan en headers; el tipo también en query. */
class PaymentSelectorRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : PaymentSelectorUseCase {
    override suspend fun entryPoint(xType: String?, branchId: String?, origin: String?): PaymentSelectorResponse {
        val headers = buildMap {
            xType?.let { put("x-type", it) }
            branchId?.let { put("x-branch-id", it) }
            origin?.let { put("x-origin", it) }
        }
        return events.reportingError("PaymentSelectorEntryPoint - Get", CIAEndpoints.PaymentSelector.PATH) {
            httpClient.get<PaymentSelectorResponse>(
                CIAEndpoints.PaymentSelector.PATH,
                query = xType?.let { mapOf("xType" to it) } ?: emptyMap(),
                headers = headers,
            )
        }
    }
}
