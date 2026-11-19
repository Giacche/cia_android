package com.ueno.cia.feature.validatelimit.data

import com.ueno.cia.core.model.CongratsScreenModel
import com.ueno.cia.core.model.CpfScreenModel
import com.ueno.cia.core.model.InputAmountScreenModel
import com.ueno.cia.core.model.LinkPreConfirmModel
import kotlinx.serialization.Serializable

/** `GET /bff/smartcharge/v2/validate`: trae embebidas las pantallas siguientes del cobro. */
@Serializable
data class ValidateLimitResponse(
    val amountScreen: InputAmountScreenModel,
    val cpfScreen: CpfScreenModel? = null,
    val linkPreConfirmScreen: LinkPreConfirmModel? = null,
    val transactionLimitScreen: CongratsScreenModel? = null,
)
