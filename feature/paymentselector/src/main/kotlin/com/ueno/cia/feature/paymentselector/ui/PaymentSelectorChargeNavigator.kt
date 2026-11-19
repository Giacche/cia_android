package com.ueno.cia.feature.paymentselector.ui

import com.ueno.cia.core.analytics.HomeAnalytics
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.HomeOptionKind
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.core.model.LinkButtonModel
import com.ueno.cia.core.navigation.CIARoute

/**
 * Equivale a `PaymentSelectorChargeNavigator`: de la opción elegida (o la acción de la alerta) a la ruta de destino,
 * registrando los mismos eventos que Flutter (`HomeAnalytics`).
 */
class PaymentSelectorChargeNavigator(private val analytics: HomeAnalytics) {
    /** `goToInitCharge`. `null` = la opción no navega (QR internacional se resuelve en la pantalla). */
    fun route(option: HomeOptionModel): CIARoute? {
        val label = option.title
        return when (option.kind) {
            HomeOptionKind.QrUeno -> qr(ChargeProvider.UPAY, label)
            HomeOptionKind.QrBancard -> qr(ChargeProvider.BANCARD, label)
            HomeOptionKind.QrDepay -> qr(ChargeProvider.DEPAY, label)
            HomeOptionKind.QrPix -> qr(ChargeProvider.PIX, label)
            HomeOptionKind.QrSip -> qr(ChargeProvider.SIP, label)
            HomeOptionKind.QrInter -> qr(ChargeProvider.INTER, label)
            HomeOptionKind.QrBolivia -> qr(ChargeProvider.BOLIVIA, label)
            HomeOptionKind.QrPeru -> qr(ChargeProvider.PERU, label)
            HomeOptionKind.QrColombia -> qr(ChargeProvider.COLOMBIA, label)
            HomeOptionKind.Link -> {
                analytics.linkStart(label)
                CIARoute.ValidateLimitEntryPoint(ChargeType.LINK, ChargeProvider.LINK, canPop = true)
            }
            HomeOptionKind.LinkList -> {
                analytics.myLinksTap(label)
                CIARoute.LinkList
            }
            // Tap to Phone no se elige desde este selector
            HomeOptionKind.Ttp -> CIARoute.TapToPhone
            HomeOptionKind.QrInternacional -> null
        }
    }

    /** `homeRucValidation`. */
    fun route(alertAction: LinkButtonModel): CIARoute? = when (alertAction.action) {
        "UPDATE_RUC" -> {
            analytics.validateRucStart(alertAction.text)
            CIARoute.TaxInfo
        }
        "UPDATE_FIELD" -> {
            analytics.updateFieldStart(alertAction.text)
            CIARoute.UpdateField
        }
        else -> null
    }

    /** `setCanPop(true)`: el back del Monto vuelve al selector en vez de ir a la Home. */
    private fun qr(provider: ChargeProvider, label: String): CIARoute {
        analytics.qrStart(label, provider)
        return CIARoute.ValidateLimitEntryPoint(ChargeType.QR, provider, canPop = true)
    }
}
