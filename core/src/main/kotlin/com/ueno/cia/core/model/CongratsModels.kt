package com.ueno.cia.core.model

import kotlinx.serialization.Serializable

// features/congrats/models (Flutter). Pantalla genérica de resultado: casi todos los flujos terminan acá.

@Serializable
data class CongratsScreenModel(
    val type: String,
    val tag: String,
    val appBarTitle: String,
    val primaryButton: ButtonModel,
    val secondaryButton: ButtonModel? = null,
    val data: CongratsScreenDataModel,
)

@Serializable
data class CongratsScreenDataModel(
    val feedbackScreenType: String,
    val semanticStatus: String,
    val title: String,
    val description: String? = null,
    val illustration: String? = null,
    val card: CongratsCardModel? = null,
)

@Serializable
data class CongratsCardModel(
    val title: String,
    val items: List<CongratsCardItemModel>,
)

@Serializable
data class CongratsCardItemModel(
    val label: String,
    val value: String,
    val subtitle: String? = null,
    val type: String? = null,
    val shareDescription: String? = null,
    val actionButton: CongratsCardItemActionButtonModel? = null,
)

@Serializable
data class CongratsCardItemActionButtonModel(
    val action: String,
    val icon: String,
    val tag: String,
    val content: String,
)

/** `CongratsOriginEnum`: define qué hacen los botones de la pantalla de resultado. */
@Serializable
enum class CongratsOrigin {
    TTP, QR, LINK, PROFILE, ONBOARDING, VALIDATE_RUC, UPDATE_FIELD, SUBSCRIPTION, TRANSACTION_LIMIT, HOME_LAYOUT,
}

@Serializable
data class CongratsInput(
    val screen: CongratsScreenModel,
    /** `null` cuando Flutter no informa origen (ej. error al generar el QR). */
    val origin: CongratsOrigin? = null,
    val receipt: ReceiptScreenModel? = null,
)
