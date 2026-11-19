package com.ueno.cia.feature.qr.domain

import com.ueno.cia.core.model.CongratsConfirmResponse
import com.ueno.cia.feature.qr.data.QRConfirmRequest
import com.ueno.cia.feature.qr.data.QRGenerateRequest
import com.ueno.cia.feature.qr.data.QRGenerateResponse

interface QRGenerateUseCase {
    suspend fun generate(request: QRGenerateRequest): QRGenerateResponse
    suspend fun confirm(request: QRConfirmRequest): CongratsConfirmResponse

    /** Avisa a `/events` que el usuario volvió a editar el monto. */
    suspend fun notifyEditQR()
}
