package com.ueno.cia.feature.detail.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.detail.domain.DetailUseCase

/** `detail_repository_impl.dart`. */
class DetailRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : DetailUseCase {
    override suspend fun getDetail(ticketNumber: String, channel: String?): DetailResponse {
        val path = CIAEndpoints.Detail.path(ticketNumber)
        return events.reportingError("Get Detail", path) {
            httpClient.get<DetailResponse>(path, channel?.let { mapOf("channel" to it) } ?: emptyMap())
        }
    }
}
