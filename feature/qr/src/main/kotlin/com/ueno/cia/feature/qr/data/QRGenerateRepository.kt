package com.ueno.cia.feature.qr.data

import com.ueno.cia.core.CIAEndpoints
import com.ueno.cia.core.events.EventsReporting
import com.ueno.cia.core.events.reportingError
import com.ueno.cia.core.model.CongratsConfirmDto
import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.core.network.CIAHttpClient
import com.ueno.cia.feature.qr.domain.QRGenerateUseCase

/** `qr_generate_repository.dart`. El `type` de los eventos lleva el proveedor. */
class QRGenerateRepository(
    private val httpClient: CIAHttpClient,
    private val events: EventsReporting,
) : QRGenerateUseCase {
    override suspend fun generate(request: QRGenerateRequest): QRGenerateResponse =
        events.reportingError("QR Generate - ${request.provider}", CIAEndpoints.QR.GENERATE) {
            httpClient.post<QRGenerateRequest, QRGenerateResponse>(CIAEndpoints.QR.GENERATE, request)
        }

    override suspend fun confirm(request: QRConfirmRequest): CongratsConfirmResponse =
        events.reportingError("QR Confirm - ${request.provider.raw}", CIAEndpoints.QR.CONFIRM) {
            httpClient.post<QRConfirmRequest, CongratsConfirmDto>(CIAEndpoints.QR.CONFIRM, request).toResponse()
        }

    override suspend fun notifyEditQR() {
        runCatching {
            events.notify(com.ueno.cia.core.events.RepositoryError("USER_ACTION", "User edit QR", "QR Edit"))
        }
    }
}
