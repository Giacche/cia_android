package com.ueno.cia.feature.paymentselector.domain

import com.ueno.cia.core.model.PaymentSelectorResponse

interface PaymentSelectorUseCase {
    suspend fun entryPoint(xType: String?, branchId: String?, origin: String?): PaymentSelectorResponse
}
