package com.ueno.cia.feature.link.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.model.CongratsConfirmDto
import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.link.domain.LinkGenerateUseCase
import com.ueno.cia.feature.link.domain.LinkListUseCase

/** `link_generate_repository.dart` y `link_list_repository.dart`. */
class LinkRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : LinkGenerateUseCase, LinkListUseCase {
    override suspend fun generate(request: LinkGenerateRequest): CongratsConfirmResponse =
        events.reportingError("Link Generate - Post", CIAEndpoints.Link.GENERATE) {
            httpClient.post<LinkGenerateRequest, CongratsConfirmDto>(CIAEndpoints.Link.GENERATE, request).toResponse()
        }

    /** `status`, `page` y `size` viajan como query. */
    override suspend fun list(status: String, page: Int, size: Int): LinkListResponse =
        events.reportingError("Link - Get List", CIAEndpoints.Link.LIST) {
            httpClient.get<LinkListResponse>(
                CIAEndpoints.Link.LIST,
                mapOf("status" to status, "page" to page.toString(), "size" to size.toString()),
            )
        }
}
