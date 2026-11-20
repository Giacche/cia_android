package com.ueno.cia.feature.qr.data

import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsScreenModel
import com.ueno.cia.core.model.QRTimeoutModel
import com.ueno.cia.core.util.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// features/qr/screens/generate/models (Flutter).

@Serializable
data class QRGenerateResponse(
    val qrScreen: QRScreenModel,
    val timeoutScreen: QRTimeoutModel,
    val errorScreen: CongratsScreenModel? = null,
)

@Serializable
data class QRScreenModel(
    val type: String,
    val appBarTitle: String,
    val tag: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val qrCode: QRCodeModel,
    val bottomSheet: QRBottomSheetModel? = null,
    val summaryInfo: QRSummaryInfoModel,
    val primaryButton: ButtonModel? = null,
    val secondaryButton: ButtonModel,
) {
    /** `getProviderLabel`: etiqueta del proveedor elegido (o la de la primera opción si no está). */
    fun providerLabel(provider: ChargeProvider): String {
        val options = bottomSheet?.options ?: return ""
        val first = options.firstOrNull() ?: return ""
        return options.firstOrNull { it.providerType == provider }?.label ?: first.label
    }
}

@Serializable
data class QRCodeModel(
    val provider: String,
    val referenceCode: String,
    /** PNG en base64. */
    val image: String,
    /** Segundos de vida del QR. */
    val timeout: Int,
    /** Segundos entre consultas de confirmación. */
    val interval: Int,
)

@Serializable
data class QRBottomSheetModel(
    val title: String,
    val options: List<QROptionModel>? = null,
)

@Serializable
data class QROptionModel(
    val provider: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val label: String,
    val tag: String,
) {
    /** Mismo mapeo que Flutter: cualquier otro valor cae en UPAY. */
    val providerType: ChargeProvider
        get() = when (provider) {
            "BANCARD" -> ChargeProvider.BANCARD
            "DEPAY" -> ChargeProvider.DEPAY
            else -> ChargeProvider.UPAY
        }
}

@Serializable
data class QRSummaryInfoModel(
    val amountTitle: String,
    val amountValueTemplate: String,
)

// region Requests

/** Flutter manda `documentNumber: null` explícito cuando no hay CPF (ver `CIARequestJson`). */
@Serializable
data class QRGenerateRequest(
    val currency: String,
    val type: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val provider: String,
    @SerialName("documentNumber") val cpfNumber: String? = null,
)

@Serializable
data class QRConfirmRequest(
    @SerialName("type") val chargeType: ChargeType,
    val provider: ChargeProvider,
    val referenceCode: String,
)

// endregion
