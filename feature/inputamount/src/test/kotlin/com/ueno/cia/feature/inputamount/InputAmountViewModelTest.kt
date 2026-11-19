package com.ueno.cia.feature.inputamount

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.InputAmountScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.InputAmountInput
import com.ueno.cia.feature.inputamount.ui.InputAmountDestination
import com.ueno.cia.feature.inputamount.ui.InputAmountViewModel
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

/** Solo el tramo del validate que interesa: la pantalla de monto. */
@Serializable
private data class AmountOnly(val amountScreen: InputAmountScreenModel)

class InputAmountViewModelTest {
    private val events = mutableListOf<AnalyticsEvent>()

    private fun screen(name: String = "bff_get_validate_qr_response.json") =
        Contratos.decode(AmountOnly.serializer(), "validate", name).amountScreen

    private fun viewModel(
        screen: InputAmountScreenModel = screen(),
        chargeType: ChargeType = ChargeType.QR,
        provider: ChargeProvider = ChargeProvider.UPAY,
        canPop: Boolean = false,
    ) = InputAmountViewModel(
        InputAmountInput(screen, chargeType, provider, canPop = canPop),
        Analytics(AnalyticsTracking { events += it }),
    )

    @Test
    fun `arranca con el boton deshabilitado y sin resumen`() {
        val state = viewModel().state.value
        assertTrue(state.buttonDisabled)
        assertFalse(state.showFooterSummary)
        assertEquals(BigDecimal.ZERO, state.amount)
    }

    @Test
    fun `un monto valido habilita el boton`() {
        val vm = viewModel()
        vm.didChangeAmount(BigDecimal(50000))
        assertFalse(vm.state.value.buttonDisabled)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `por encima del maximo muestra el error y bloquea`() {
        val vm = viewModel()
        val screen = vm.state.value.screen
        vm.didChangeAmount(screen.maxAmount + BigDecimal.ONE)
        assertEquals(screen.data.maxAmountErrorMessage, vm.state.value.errorMessage)
        assertTrue(vm.state.value.buttonDisabled)
    }

    @Test
    fun `por debajo del minimo muestra el error y bloquea`() {
        val vm = viewModel()
        val screen = vm.state.value.screen
        vm.didChangeAmount(screen.minAmount - BigDecimal.ONE)
        assertEquals(screen.data.minAmountErrorMessage, vm.state.value.errorMessage)
        assertTrue(vm.state.value.buttonDisabled)
    }

    @Test
    fun `volver a cero limpia el error y bloquea sin error`() {
        val vm = viewModel()
        vm.didChangeAmount(BigDecimal(999999999))
        vm.didChangeAmount(BigDecimal.ZERO)
        assertNull(vm.state.value.errorMessage)
        assertTrue(vm.state.value.buttonDisabled)
        assertFalse(vm.state.value.showSummary)
    }

    @Test
    fun `calcula la comision y lo que recibe el comercio`() {
        val vm = viewModel(screen("ios_get_validate_qr_sin_cpf_response.json"))
        // Este contrato trae los textos del resumen vacíos: el resumen no se muestra
        vm.didChangeAmount(BigDecimal(100000))
        assertFalse(vm.state.value.showFooterSummary)

        val withSummary = screen().copy(
            summaryInfo = screen().summaryInfo.copy(
                amountTitle = "Recibís",
                amountValueTemplate = "{amount}",
                commissionTitle = "Comisión",
                commissionValueTemplate = "{commission}% ({commission_amount})",
            ),
        )
        val vm2 = viewModel(withSummary)
        vm2.didChangeAmount(BigDecimal(100000))
        val state = vm2.state.value
        assertTrue(state.showFooterSummary)
        // 5,5 % de 100.000 = 5.500; el comercio recibe 94.500
        assertEquals("5.5% (Gs. 5.500)", state.commissionValue)
        assertEquals("Gs. 94.500", state.totalAmountValue)
    }

    @Test
    fun `la comision se imprime como double de Dart`() {
        assertEquals("5.0", InputAmountViewModel.dartDoubleString(BigDecimal("5")))
        assertEquals("5.5", InputAmountViewModel.dartDoubleString(BigDecimal("5.50")))
        assertEquals("0.0", InputAmountViewModel.dartDoubleString(BigDecimal.ZERO))
    }

    @Test
    fun `continuar en QR navega a generar y registra analytics`() = runBlocking {
        val vm = viewModel(provider = ChargeProvider.BANCARD)
        vm.didChangeAmount(BigDecimal(50000))
        vm.didTapContinue()
        val destination = vm.destinations.first()
        assertIs<InputAmountDestination.Push>(destination)
        val route = assertIs<CIARoute.QrGenerate>(destination.route)
        assertEquals(BigDecimal(50000), route.input.amount)
        assertEquals(ChargeProvider.BANCARD, route.input.provider)
        val event = events.single { it.kind == AnalyticsEvent.Kind.FLOW_STEP_CONTINUE }
        assertEquals("ingresar_monto", event.parameters["stepName"])
        assertEquals("qr_bancard_inicio", event.parameters["previousStepName"])
        assertEquals("qr_bancard", event.subFlow)
    }

    @Test
    fun `el back vuelve a la Home salvo que se haya llegado desde un selector`() = runBlocking {
        val fromHome = viewModel(canPop = false)
        fromHome.didTapBack()
        assertEquals(InputAmountDestination.GoBackToHome, fromHome.destinations.first())

        val fromSelector = viewModel(canPop = true)
        fromSelector.didTapBack()
        assertEquals(InputAmountDestination.Back, fromSelector.destinations.first())
    }

    @Test
    fun `continuar en link sin pre-confirmacion no hace nada`() {
        val vm = viewModel(chargeType = ChargeType.LINK, provider = ChargeProvider.LINK)
        vm.didChangeAmount(BigDecimal(50000))
        vm.didTapContinue()
        assertTrue(events.isEmpty())
    }
}
