package com.ueno.cia.feature.congrats.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.model.CongratsCardItemModel
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.model.CongratsScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CIARouteDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface CongratsDestination {
    data class Push(val route: CIARoute) : CongratsDestination
    data class Reset(val route: CIARoute) : CongratsDestination

    /** `_popOrGoHome`: vuelve una pantalla si hay a dónde, si no a la Home. */
    data object PopOrGoHome : CongratsDestination
    data object GoHome : CongratsDestination
    data class OpenUrl(val url: String) : CongratsDestination
    data class CallPhone(val number: String) : CongratsDestination
    data class Share(val text: String) : CongratsDestination
    data class Copy(val value: String) : CongratsDestination
}

/** Tipo visual (`feedbackScreenType`); cualquier otro valor es la pantalla de error genérica. */
enum class CongratsKind { Semantic, Neutral, Error }

/**
 * Equivale a `CongratsNotifier` + las acciones de `CongratsScreen`. Pantalla de resultado genérica: qué hacen los
 * botones depende de `feedbackScreenType`, `semanticStatus` y el origen del flujo.
 */
class CongratsViewModel(
    val input: CongratsInput,
    private val analytics: Analytics,
) : ViewModel() {
    val screen: CongratsScreenModel get() = input.screen

    private val _destinations = Channel<CongratsDestination>(Channel.BUFFERED)
    val destinations: Flow<CongratsDestination> = _destinations.receiveAsFlow()

    val kind: CongratsKind
        get() = when (screen.data.feedbackScreenType) {
            "semantic" -> CongratsKind.Semantic
            "neutral" -> CongratsKind.Neutral
            else -> CongratsKind.Error
        }

    private var started = false

    /** Los flujos de link y suscripción miden el éxito al abrir la pantalla (el notifier lo hace en `build`). */
    fun start() {
        if (started) return
        started = true
        when (input.origin) {
            CongratsOrigin.LINK -> trackSuccess("link_success", "link_revisar_detalle", "link", "cobros_link_success")
            CongratsOrigin.SUBSCRIPTION ->
                trackSuccess("suscripcion_success", "suscripcion_revisar_detalle", "suscripcion", "cobros_suscripcion_success")
            else -> Unit
        }
    }

    // region Botones

    fun didTapPrimary() = when (kind) {
        CongratsKind.Semantic -> semanticPrimary()
        CongratsKind.Neutral -> neutralPrimary()
        CongratsKind.Error -> emit(CongratsDestination.GoHome)
    }

    fun didTapSecondary() = when (kind) {
        CongratsKind.Semantic, CongratsKind.Error -> emit(CongratsDestination.GoHome)
        CongratsKind.Neutral -> neutralSecondary()
    }

    /** Botón de un ítem de la tarjeta (COPY / SHARE). */
    fun didTapCardAction(item: CongratsCardItemModel) {
        val button = item.actionButton ?: return
        when (button.action) {
            "COPY" -> emit(CongratsDestination.Copy(button.content))
            "SHARE" -> emit(CongratsDestination.Share(item.shareDescription.orEmpty()))
        }
    }

    // endregion

    // region Reglas

    private val isSuccess: Boolean get() = screen.data.semanticStatus == "success"

    private fun semanticPrimary() {
        if (!isSuccess) return emit(CongratsDestination.PopOrGoHome)

        if (input.origin == CongratsOrigin.QR) {
            analytics.buttonTap(
                label = screen.primaryButton.text,
                destination = CIARouteDestination.RECEIPT,
                subFlow = "qr_ueno",
                extra = mapOf(Analytics.SCREEN_NAME_KEY to "cobros_qr_success"),
            )
        }
        if (input.origin == CongratsOrigin.LINK) {
            val linkItem = screen.data.card?.items?.firstOrNull { it.type == LINK_ITEM_TYPE }
            emit(CongratsDestination.Share(linkItem?.shareDescription.orEmpty()))
        }
        val receipt = input.receipt ?: return
        emit(CongratsDestination.Push(CIARoute.Receipt(receipt)))
    }

    private fun neutralPrimary() {
        if (input.origin == CongratsOrigin.UPDATE_FIELD && isSuccess) {
            return emit(CongratsDestination.Reset(CIARoute.Profile))
        }
        val url = httpsUrl(screen.primaryButton.deeplink)
        when {
            url != null -> emit(CongratsDestination.OpenUrl(url))
            isSuccess -> emit(CongratsDestination.GoHome)
            else -> emit(CongratsDestination.PopOrGoHome)
        }
    }

    private fun neutralSecondary() {
        val deeplink = screen.secondaryButton?.deeplink
        if (input.origin == CongratsOrigin.TRANSACTION_LIMIT) {
            return emit(CongratsDestination.CallPhone(deeplink.orEmpty()))
        }
        val url = httpsUrl(deeplink)
        if (deeplink != "HOME" && url != null) emit(CongratsDestination.OpenUrl(url)) else emit(CongratsDestination.GoHome)
    }

    private fun httpsUrl(deeplink: String?): String? = deeplink?.takeIf { it.startsWith("https") }

    private fun trackSuccess(step: String, previous: String, subFlow: String, screenName: String) {
        analytics.flowSuccess(
            stepName = step,
            previousStepName = previous,
            status = "ok",
            subFlow = subFlow,
            extra = mapOf(Analytics.SCREEN_NAME_KEY to screenName),
        )
    }

    private fun emit(destination: CongratsDestination) {
        _destinations.trySend(destination)
    }

    // endregion

    private companion object {
        const val LINK_ITEM_TYPE = "LINK"
    }
}
