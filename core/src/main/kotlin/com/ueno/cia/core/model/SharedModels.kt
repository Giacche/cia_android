package com.ueno.cia.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Modelos compartidos (lib/src/shared/models/ en Flutter). Mismos nombres de campo y nulabilidad.

@Serializable
data class ButtonModel(
    val text: String,
    val tag: String,
    val deeplink: String? = null,
    val icon: String? = null,
)

@Serializable
data class LinkButtonModel(
    val text: String,
    val tag: String,
    val deeplink: String? = null,
    val action: String? = null,
)

@Serializable
data class BadgeModel(
    val type: String,
    val icon: String? = null,
    val text: String,
)

@Serializable
data class EmptyStateModel(
    val illustration: String,
    val title: String,
    val subtitle: String,
)

@Serializable
data class InlineMessageModel(
    val title: String,
    val message: String,
)

@Serializable
data class TycModel(
    val tag: String,
    val text: String,
    val linkText: String,
    val linkUrl: String,
)

/** Equivale a `ChargeTypeEnum`. */
@Serializable
enum class ChargeType {
    @SerialName("TTP")
    TTP,

    @SerialName("QR")
    QR,

    @SerialName("LINK")
    LINK,
}

/** Equivale a `ChargeProviderEnum`. Un valor desconocido del BFF se interpreta como [UNKNOWN] (ver [fromRaw]). */
@Serializable
enum class ChargeProvider(val raw: String) {
    @SerialName("UPAY")
    UPAY("UPAY"),

    @SerialName("BANCARD")
    BANCARD("BANCARD"),

    @SerialName("DEPAY")
    DEPAY("DEPAY"),

    @SerialName("PIX")
    PIX("PIX"),

    @SerialName("LINK")
    LINK("LINK"),

    @SerialName("SIP")
    SIP("SIP"),

    @SerialName("INTER")
    INTER("INTER"),

    @SerialName("QR-BO")
    BOLIVIA("QR-BO"),

    @SerialName("QR-PE")
    PERU("QR-PE"),

    @SerialName("QR-CO")
    COLOMBIA("QR-CO"),

    @SerialName("UNKNOWN")
    UNKNOWN("UNKNOWN"),
    ;

    companion object {
        fun fromRaw(raw: String): ChargeProvider = entries.firstOrNull { it.raw == raw.uppercase() } ?: UNKNOWN
    }
}
