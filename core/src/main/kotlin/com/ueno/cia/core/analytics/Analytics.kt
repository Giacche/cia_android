package com.ueno.cia.core.analytics

import com.ueno.cia.core.model.ChargeProvider

/** Contrato con el host (Amplitude). */
data class AnalyticsEvent(
    val kind: Kind,
    val squadName: String,
    val flow: String,
    val subFlow: String,
    val parameters: Map<String, String>,
    val extraParams: Map<String, String>,
) {
    enum class Kind { CUSTOM_SCREEN_VIEW, FLOW_START, FLOW_STEP_CONTINUE, FLOW_SUCCESS, BUTTON_TAP, ERROR }
}

fun interface AnalyticsTracking {
    fun track(event: AnalyticsEvent)
}

object NoOpAnalyticsTracking : AnalyticsTracking {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Equivale a `Analytics` (analytics_helper.dart). */
class Analytics(private val tracker: AnalyticsTracking) {
    fun flowStart(stepName: String, label: String, destination: String, subFlow: String, extra: Map<String, String>) =
        track(
            AnalyticsEvent.Kind.FLOW_START, subFlow, extra,
            mapOf("stepName" to stepName, "eventLabel" to label, "elementDestination" to destination),
        )

    fun screenView(subFlow: String, screenName: String, extra: Map<String, String> = emptyMap()) =
        track(AnalyticsEvent.Kind.CUSTOM_SCREEN_VIEW, subFlow, mapOf(SCREEN_NAME_KEY to screenName) + extra, emptyMap())

    fun flowContinue(
        stepName: String,
        previousStepName: String,
        label: String,
        destination: String,
        subFlow: String,
        extra: Map<String, String>,
    ) = track(
        AnalyticsEvent.Kind.FLOW_STEP_CONTINUE, subFlow, extra,
        mapOf(
            "stepName" to stepName,
            "previousStepName" to previousStepName,
            "eventLabel" to label,
            "elementDestination" to destination,
        ),
    )

    /** `Analytics.trackFlowSuccess`: `status` es `ok`, `error` o `N/A`. */
    fun flowSuccess(stepName: String, previousStepName: String, status: String, subFlow: String, extra: Map<String, String>) =
        track(
            AnalyticsEvent.Kind.FLOW_SUCCESS, subFlow, extra,
            mapOf("stepName" to stepName, "previousStepName" to previousStepName, "status" to status),
        )

    /** `Analytics.trackErrorEvent` con `errorType` `api`. */
    fun error(serviceName: String, errorCode: Int, errorMessage: String, subFlow: String, extra: Map<String, String>) =
        track(
            AnalyticsEvent.Kind.ERROR, subFlow, extra,
            mapOf(
                "serviceName" to serviceName,
                "errorCode" to errorCode.toString(),
                "errorType" to "api",
                "errorMessage" to errorMessage,
            ),
        )

    fun buttonTap(label: String, destination: String, subFlow: String, extra: Map<String, String>) =
        track(
            AnalyticsEvent.Kind.BUTTON_TAP, subFlow, extra,
            mapOf("eventLabel" to label, "elementDestination" to destination, "eventCategory" to BUTTON_CATEGORY),
        )

    private fun track(kind: AnalyticsEvent.Kind, subFlow: String, extra: Map<String, String>, parameters: Map<String, String>) {
        tracker.track(AnalyticsEvent(kind, SQUAD_NAME, FLOW, subFlow, parameters, extra))
    }

    companion object {
        const val FLOW = "cobros"
        const val SQUAD_NAME = "ChargeInApp"
        const val BUTTON_CATEGORY = "boton"
        const val SCREEN_NAME_KEY = "screen_name"
    }
}

/**
 * Nombres de pasos y subflows de QR (`QrAnalytics` en Flutter). Los usan Home, Monto y QR: no se pueden
 * cambiar sin romper los dashboards.
 */
object QRAnalyticsNames {
    const val INPUT_AMOUNT_STEP = "ingresar_monto"
    const val INPUT_AMOUNT_SCREEN_NAME = "cobros_ingresar_monto"

    fun startStep(provider: ChargeProvider): String = when (provider) {
        ChargeProvider.BANCARD -> "qr_bancard_inicio"
        ChargeProvider.DEPAY -> "qr_depay_inicio"
        ChargeProvider.PIX -> "qr_pix_inicio"
        ChargeProvider.SIP -> "qr_sip_inicio"
        ChargeProvider.INTER -> "qr_inter_inicio"
        ChargeProvider.BOLIVIA -> "qr_bolivia_inicio"
        ChargeProvider.PERU -> "qr_peru_inicio"
        ChargeProvider.COLOMBIA -> "qr_colombia_inicio"
        ChargeProvider.UPAY, ChargeProvider.LINK, ChargeProvider.UNKNOWN -> "qr_ueno_inicio"
    }

    fun subFlow(provider: ChargeProvider): String = when (provider) {
        ChargeProvider.BANCARD -> "qr_bancard"
        ChargeProvider.DEPAY -> "qr_depay"
        ChargeProvider.PIX -> "qr_pix"
        ChargeProvider.SIP -> "qr_sip"
        ChargeProvider.INTER -> "qr_inter"
        ChargeProvider.BOLIVIA -> "qr_bolivia"
        ChargeProvider.PERU -> "qr_peru"
        ChargeProvider.COLOMBIA -> "qr_colombia"
        ChargeProvider.UPAY, ChargeProvider.LINK, ChargeProvider.UNKNOWN -> "qr_ueno"
    }
}
