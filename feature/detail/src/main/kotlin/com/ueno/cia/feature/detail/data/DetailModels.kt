package com.ueno.cia.feature.detail.data

import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.DetailCardModel
import com.ueno.cia.core.model.ReceiptScreenModel
import kotlinx.serialization.Serializable

// features/detail/screens/detail/models (Flutter).

@Serializable
data class DetailResponse(
    val detailScreen: DetailScreenModel,
    val receiptScreen: ReceiptScreenModel? = null,
)

@Serializable
data class DetailScreenModel(
    val type: String,
    val tag: String,
    val appBarTitle: String,
    val date: String,
    val title: String,
    val amount: String,
    val stepperCard: DetailStepperCardModel,
    val paymentTypeCard: DetailPaymentTypeCardModel,
    val detailCard: DetailCardModel,
    val discountsCard: DetailCardModel? = null,
    val primaryButton: ButtonModel? = null,
    val secondaryButton: ButtonModel? = null,
)

@Serializable
data class DetailStepperCardModel(
    val tag: String,
    val title: String,
    val items: List<DetailStepperCardItemModel>,
)

@Serializable
data class DetailStepperCardItemModel(
    val title: String,
    val description: String,
    /** `completed`, `info`, `error`, `warning` u otro (deshabilitado). */
    val status: String,
)

@Serializable
data class DetailPaymentTypeCardModel(
    val title: String,
    val subtitle: String,
    val detail: String? = null,
    val icon: String,
)
