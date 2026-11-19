package com.ueno.cia.core.analytics

import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.navigation.CIARouteDestination

/** Equivale a `HomeAnalytics` (home_analytics.dart). Mismos stepName / subFlow / destinos. */
class HomeAnalytics(private val analytics: Analytics) {
    private val extra = mapOf(Analytics.SCREEN_NAME_KEY to SCREEN_NAME)

    fun qrStart(label: String, provider: ChargeProvider) = analytics.flowStart(
        stepName = QRAnalyticsNames.startStep(provider),
        label = label,
        destination = CIARouteDestination.INPUT_AMOUNT,
        subFlow = QRAnalyticsNames.subFlow(provider),
        extra = extra,
    )

    fun linkStart(label: String) = analytics.flowStart(
        "link_crear_inicio", label, CIARouteDestination.INPUT_AMOUNT, "link", extra,
    )

    fun myLinksTap(label: String) = analytics.buttonTap(label, CIARouteDestination.LINK_LIST, "link", extra)

    fun validateRucStart(label: String) = analytics.flowStart(
        "validacion_ruc_informacion_inicio", label, CIARouteDestination.TAX_INFO, "validacion_ruc", extra,
    )

    fun updateFieldStart(label: String) = analytics.flowStart(
        "actualizacion_rubro_inicio", label, CIARouteDestination.UPDATE_FIELD, "actualizacion_rubro", extra,
    )

    fun linkOnboardingStart(label: String) = analytics.flowStart(
        "link_onboarding_inicio", label, CIARouteDestination.PRODUCTS_ONBOARDING, "link", extra,
    )

    fun subscriptionOnboardingStart(label: String) = analytics.flowStart(
        "suscripcion_onboarding_inicio", label, CIARouteDestination.PRODUCTS_ONBOARDING, "suscripcion", extra,
    )

    fun editProfileTap(label: String) = analytics.buttonTap(label, CIARouteDestination.PROFILE_NAME, "perfil", extra)

    fun helpTap(label: String) = analytics.buttonTap(label, CIARouteDestination.HELP_CENTER, SUB_FLOW, extra)

    fun movementsTap(label: String) = analytics.buttonTap(label, CIARouteDestination.MOVEMENTS, SUB_FLOW, extra)

    companion object {
        const val SCREEN_NAME = "cobros_home"
        const val SUB_FLOW = "home"
    }
}
