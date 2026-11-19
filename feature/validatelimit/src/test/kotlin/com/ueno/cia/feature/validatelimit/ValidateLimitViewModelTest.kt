package com.ueno.cia.feature.validatelimit

import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.network.APIError
import com.ueno.cia.feature.validatelimit.data.ValidateLimitResponse
import com.ueno.cia.feature.validatelimit.domain.ValidateLimitUseCase
import com.ueno.cia.feature.validatelimit.ui.ValidateLimitDestination
import com.ueno.cia.feature.validatelimit.ui.ValidateLimitState
import com.ueno.cia.feature.validatelimit.ui.ValidateLimitViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ValidateLimitViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun contrato(name: String) = Contratos.decode(ValidateLimitResponse.serializer(), "validate", name)

    private fun viewModel(
        response: ValidateLimitResponse? = null,
        chargeType: ChargeType = ChargeType.QR,
        provider: ChargeProvider = ChargeProvider.UPAY,
        canPop: Boolean = false,
    ) = ValidateLimitViewModel(
        chargeType, provider, canPop,
        object : ValidateLimitUseCase {
            override suspend fun validateLimit(chargeType: ChargeType, provider: ChargeProvider) =
                response ?: throw APIError.Server(500, null, "boom")
        },
    )

    @Test
    fun `sin CPF ni tope va directo al monto`() = runTest(dispatcher) {
        val vm = viewModel(contrato("ios_get_validate_qr_sin_cpf_response.json"), canPop = true)
        vm.start()
        advanceUntilIdle()
        val destination = vm.destinations.first()
        assertIs<ValidateLimitDestination.Replace>(destination)
        val route = assertIs<CIARoute.InputAmount>(destination.route)
        assertEquals(ChargeType.QR, route.input.chargeType)
        assertEquals(ChargeProvider.UPAY, route.input.provider)
        assertEquals(true, route.input.canPop)
        assertNull(route.input.cpfNumber)
    }

    @Test
    fun `con pantalla de CPF va primero al CPF`() = runTest(dispatcher) {
        val vm = viewModel(contrato("bff_get_validate_qr_response.json"))
        vm.start()
        advanceUntilIdle()
        val destination = vm.destinations.first()
        assertIs<CIARoute.Cpf>((destination as ValidateLimitDestination.Replace).route)
    }

    @Test
    fun `el tope transaccional gana sobre el CPF y el monto`() = runTest(dispatcher) {
        val vm = viewModel(contrato("bff_get_validate_transaction_limit_response.json"))
        vm.start()
        advanceUntilIdle()
        val route = (vm.destinations.first() as ValidateLimitDestination.Replace).route
        val congrats = assertIs<CIARoute.Congrats>(route)
        assertEquals(CongratsOrigin.TRANSACTION_LIMIT, congrats.input.origin)
    }

    @Test
    fun `si falla muestra el error y volver sale`() = runTest(dispatcher) {
        val vm = viewModel(response = null)
        vm.start()
        advanceUntilIdle()
        assertEquals(ValidateLimitState.Failed, vm.state.value)
        vm.didTapBackFromError()
        assertEquals(ValidateLimitDestination.Back, vm.destinations.first())
    }
}
