package com.ueno.cia.core.model

import kotlinx.serialization.Serializable

// features/payment_selector/models (Flutter).

/** `PaymentSelectorResponseModel`: `{ "paymentEntryPoint": { ... } }` dentro del envelope `data`. */
@Serializable
data class PaymentSelectorResponse(val paymentEntryPoint: PaymentSelectorScreenModel? = null)

/** Pantalla de selección: métodos de cobro o países de QR internacional. */
@Serializable
data class PaymentSelectorScreenModel(
    val tag: String,
    val appBarTitle: String,
    val type: String,
    val data: PaymentSelectorDataModel,
)

@Serializable
data class PaymentSelectorDataModel(
    val title: String,
    val options: List<HomeOptionModel>,
    val informationAlert: HomeInformationAlertModel? = null,
)
