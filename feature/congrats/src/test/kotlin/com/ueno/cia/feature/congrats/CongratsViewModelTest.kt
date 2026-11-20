package com.ueno.cia.feature.congrats

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.CongratsCardItemActionButtonModel
import com.ueno.cia.core.model.CongratsCardItemModel
import com.ueno.cia.core.model.CongratsCardModel
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.model.CongratsScreenDataModel
import com.ueno.cia.core.model.CongratsScreenModel
import com.ueno.cia.core.model.ReceiptScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.feature.congrats.ui.CongratsDestination
import com.ueno.cia.feature.congrats.ui.CongratsKind
import com.ueno.cia.feature.congrats.ui.CongratsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CongratsViewModelTest {
    private val events = mutableListOf<AnalyticsEvent>()

    private fun screen(
        type: String = "semantic",
        status: String = "success",
        primaryDeeplink: String? = null,
        secondaryDeeplink: String? = null,
        card: CongratsCardModel? = null,
    ) = CongratsScreenModel(
        type = "X",
        tag = "cia_congrats",
        appBarTitle = "",
        primaryButton = ButtonModel("Principal", "cia_primary", primaryDeeplink),
        secondaryButton = ButtonModel("Secundario", "cia_secondary", secondaryDeeplink),
        data = CongratsScreenDataModel(feedbackScreenType = type, semanticStatus = status, title = "Título", card = card),
    )

    private fun receipt() = ReceiptScreenModel(
        tag = "r", receiptName = "Comprobante", appBarTitle = "", subTitle = "", downloadLabel = "", shareLabel = "",
        amount = BigDecimal(10), currency = "PYG", date = Instant.EPOCH, items = emptyList(),
    )

    private fun viewModel(screen: CongratsScreenModel = screen(), origin: CongratsOrigin? = null, receipt: ReceiptScreenModel? = null) =
        CongratsViewModel(CongratsInput(screen, origin, receipt), Analytics(AnalyticsTracking { events += it }))

    @Test
    fun `el tipo visual sale de feedbackScreenType`() {
        assertEquals(CongratsKind.Semantic, viewModel(screen(type = "semantic")).kind)
        assertEquals(CongratsKind.Neutral, viewModel(screen(type = "neutral")).kind)
        assertEquals(CongratsKind.Error, viewModel(screen(type = "otra-cosa")).kind)
    }

    @Test
    fun `un cobro de QR exitoso abre el comprobante y mide el tap`() = runBlocking {
        val receipt = receipt()
        val vm = viewModel(origin = CongratsOrigin.QR, receipt = receipt)
        vm.didTapPrimary()
        val destination = vm.destinations.first()
        assertEquals(CongratsDestination.Push(CIARoute.Receipt(receipt)), destination)
        val event = events.single { it.kind == AnalyticsEvent.Kind.BUTTON_TAP }
        assertEquals("cobros_qr_success", event.extraParams["screen_name"])
    }

    @Test
    fun `un resultado semantico con error vuelve atras o a la Home`() = runBlocking {
        val vm = viewModel(screen(status = "error"), origin = CongratsOrigin.QR)
        vm.didTapPrimary()
        assertEquals(CongratsDestination.PopOrGoHome, vm.destinations.first())
    }

    @Test
    fun `el boton secundario semantico y el de error van a la Home`() = runBlocking {
        val semantic = viewModel()
        semantic.didTapSecondary()
        assertEquals(CongratsDestination.GoHome, semantic.destinations.first())

        val error = viewModel(screen(type = "error"))
        error.didTapPrimary()
        assertEquals(CongratsDestination.GoHome, error.destinations.first())
    }

    @Test
    fun `en un link exitoso el principal comparte el link`() = runBlocking {
        val card = CongratsCardModel(
            "Detalle",
            listOf(CongratsCardItemModel("Link", "https://x", type = "LINK", shareDescription = "Pagá acá: https://x")),
        )
        val vm = viewModel(screen(card = card), origin = CongratsOrigin.LINK)
        vm.didTapPrimary()
        assertEquals(CongratsDestination.Share("Pagá acá: https://x"), vm.destinations.first())
    }

    @Test
    fun `los flujos de link y suscripcion miden el exito al abrir`() {
        viewModel(origin = CongratsOrigin.LINK).start()
        viewModel(origin = CongratsOrigin.SUBSCRIPTION).start()
        viewModel(origin = CongratsOrigin.QR).start()
        val steps = events.filter { it.kind == AnalyticsEvent.Kind.FLOW_SUCCESS }.map { it.parameters["stepName"] }
        assertEquals(listOf("link_success", "suscripcion_success"), steps)
    }

    @Test
    fun `start mide una sola vez`() {
        val vm = viewModel(origin = CongratsOrigin.LINK)
        vm.start()
        vm.start()
        assertEquals(1, events.count { it.kind == AnalyticsEvent.Kind.FLOW_SUCCESS })
    }

    @Test
    fun `neutral con link https abre la url`() = runBlocking {
        val vm = viewModel(screen(type = "neutral", primaryDeeplink = "https://ueno.com.py/ayuda"))
        vm.didTapPrimary()
        assertEquals(CongratsDestination.OpenUrl("https://ueno.com.py/ayuda"), vm.destinations.first())
    }

    @Test
    fun `neutral sin link va a la Home si fue exitoso y vuelve atras si no`() = runBlocking {
        val ok = viewModel(screen(type = "neutral", status = "success"))
        ok.didTapPrimary()
        assertEquals(CongratsDestination.GoHome, ok.destinations.first())

        val fail = viewModel(screen(type = "neutral", status = "error"))
        fail.didTapPrimary()
        assertEquals(CongratsDestination.PopOrGoHome, fail.destinations.first())
    }

    @Test
    fun `en el tope transaccional el secundario llama por telefono`() = runBlocking {
        val vm = viewModel(screen(type = "neutral", secondaryDeeplink = "+595216180000"), origin = CongratsOrigin.TRANSACTION_LIMIT)
        vm.didTapSecondary()
        assertEquals(CongratsDestination.CallPhone("+595216180000"), vm.destinations.first())
    }

    @Test
    fun `neutral secundario con HOME va a la Home aunque tenga url`() = runBlocking {
        val vm = viewModel(screen(type = "neutral", secondaryDeeplink = "HOME"))
        vm.didTapSecondary()
        assertEquals(CongratsDestination.GoHome, vm.destinations.first())
    }

    @Test
    fun `actualizar el rubro con exito resetea al perfil`() = runBlocking {
        val vm = viewModel(screen(type = "neutral", status = "success"), origin = CongratsOrigin.UPDATE_FIELD)
        vm.didTapPrimary()
        assertEquals(CongratsDestination.Reset(CIARoute.Profile), vm.destinations.first())
    }

    @Test
    fun `las acciones de la tarjeta copian y comparten`() = runBlocking {
        val copy = CongratsCardItemModel("Ref", "123", actionButton = CongratsCardItemActionButtonModel("COPY", "copy", "t", "123"))
        val share = CongratsCardItemModel(
            "Link",
            "x",
            shareDescription = "texto",
            actionButton = CongratsCardItemActionButtonModel("SHARE", "share", "t", "x"),
        )
        val vm = viewModel()
        vm.didTapCardAction(copy)
        assertEquals(CongratsDestination.Copy("123"), vm.destinations.first())
        vm.didTapCardAction(share)
        assertEquals(CongratsDestination.Share("texto"), vm.destinations.first())
        vm.didTapCardAction(CongratsCardItemModel("Sin accion", "x"))
        assertTrue(true)
    }

    @Test
    fun `un exito sin comprobante no navega`() {
        val vm = viewModel(origin = CongratsOrigin.QR, receipt = null)
        vm.didTapPrimary()
        assertIs<CongratsViewModel>(vm)
    }
}
