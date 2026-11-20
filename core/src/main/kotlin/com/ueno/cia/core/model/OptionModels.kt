package com.ueno.cia.core.model

import kotlinx.serialization.Serializable

// Opciones de cobro y alertas que comparten la Home y el selector de cobros (features/home/screens/home/models).

@Serializable
data class HomeOptionModel(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val badge: BadgeModel,
    val tag: String,
    val deeplink: String,
    val modal: HomeModalModel? = null,
    /** Flutter: `json['disabled'] as bool? ?? false`. */
    val disabled: Boolean = false,
) {
    val kind: HomeOptionKind get() = HomeOptionKind.fromId(id)
}

@Serializable
data class HomeModalModel(val title: String, val options: List<HomeOptionModel>)

/** Equivale a `HomeOptionsEnum`. Id desconocido → [QrUeno] (default de Flutter). */
enum class HomeOptionKind {
    Ttp,
    QrUeno,
    QrBancard,
    QrDepay,
    QrPix,
    Link,
    QrSip,
    QrInter,
    QrBolivia,
    QrPeru,
    QrColombia,
    QrInternacional,
    LinkList,
    ;

    companion object {
        fun fromId(id: String): HomeOptionKind = when (id) {
            "TTP" -> Ttp
            "QR_UENO" -> QrUeno
            "QR_BANCARD" -> QrBancard
            "QR_DEPAY" -> QrDepay
            "QR_PIX" -> QrPix
            "LINK" -> Link
            "QR_SIP" -> QrSip
            "QR_INTER" -> QrInter
            "QR_BOLIVIA" -> QrBolivia
            "QR_PERU" -> QrPeru
            "QR_COLOMBIA" -> QrColombia
            "QR_INTERNACIONAL" -> QrInternacional
            "LINK_LIST" -> LinkList
            else -> QrUeno
        }
    }
}

@Serializable
data class HomeInformationAlertModel(
    val type: String,
    val description: String,
    val title: String? = null,
    val actionLink: LinkButtonModel? = null,
)

/** Flutter: 3/2, o 2.5/2 si alguna opción tiene descripción (ancho / alto de cada card de la grilla). */
fun List<HomeOptionModel>.gridAspectRatio(): Float = if (any { it.description.isNotEmpty() }) 2.5f / 2f else 3f / 2f
