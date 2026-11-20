package com.ueno.cia.feature.cpf

import com.ueno.cia.core.analytics.Analytics
import com.ueno.cia.core.analytics.AnalyticsEvent
import com.ueno.cia.core.analytics.AnalyticsTracking
import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CpfScreenModel
import com.ueno.cia.core.model.InputAmountScreenModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.CPFInput
import com.ueno.cia.feature.cpf.ui.CPFDestination
import com.ueno.cia.feature.cpf.ui.CPFViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Serializable
private data class ValidateAmount(val amountScreen: InputAmountScreenModel, val cpfScreen: CpfScreenModel? = null)

class CPFViewModelTest {
    private val events = mutableListOf<AnalyticsEvent>()

    private val response = Contratos.decode(ValidateAmount.serializer(), "validate", "bff_get_validate_qr_response.json")
    private val cpfScreen = response.cpfScreen!!

    private fun viewModel(canPop: Boolean = true) = CPFViewModel(
        CPFInput(cpfScreen, response.amountScreen, ChargeType.QR, ChargeProvider.PIX, canPop),
        Analytics(AnalyticsTracking { events += it }),
    )

    @Test
    fun `arranca con la ayuda del BFF y el boton deshabilitado`() {
        val state = viewModel().state.value
        assertEquals(cpfScreen.numberInput.helper, state.helper)
        assertFalse(state.buttonEnabled)
    }

    @Test
    fun `vacio es obligatorio`() {
        val vm = viewModel()
        vm.didChangeCpf("   ")
        assertEquals("El CPF es obligatorio.", vm.state.value.error)
        assertNull(vm.state.value.helper)
        assertFalse(vm.state.value.buttonEnabled)
    }

    @Test
    fun `un valor que no cumple la regex muestra el mensaje del BFF`() {
        val vm = viewModel()
        vm.didChangeCpf("abc")
        assertEquals(cpfScreen.numberInput.errorMessage, vm.state.value.error)
    }

    @Test
    fun `un valor valido habilita el boton y vuelve a la ayuda`() {
        val vm = viewModel()
        vm.didChangeCpf("12345678901")
        val state = vm.state.value
        assertNull(state.error)
        assertEquals(cpfScreen.numberInput.helper, state.helper)
        assertTrue(state.buttonEnabled)
    }

    @Test
    fun `continuar lleva al monto con el CPF y mide el paso`() = runBlocking {
        val vm = viewModel(canPop = true)
        vm.didChangeCpf("12345678901")
        vm.didTapContinue()
        val destination = vm.destinations.first()
        assertIs<CPFDestination.Push>(destination)
        val route = assertIs<CIARoute.InputAmount>(destination.route)
        assertEquals("12345678901", route.input.cpfNumber)
        assertEquals(ChargeProvider.PIX, route.input.provider)
        assertEquals(true, route.input.canPop)
        assertEquals("ingresar_cpf", events.single { it.kind == AnalyticsEvent.Kind.FLOW_STEP_CONTINUE }.parameters["stepName"])
    }

    @Test
    fun `no continua si el CPF es invalido`() {
        val vm = viewModel()
        vm.didChangeCpf("abc")
        vm.didTapContinue()
        assertTrue(events.isEmpty())
    }
}
