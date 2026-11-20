package com.ueno.cia.sandbox

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Pruebas de UI del sandbox con los mocks reales (contratos JSON). Verifican el recorrido completo
 * lanzador → Home y que los controles tengan etiqueta para los lectores de pantalla.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SandboxUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun openHome() {
        compose.onNodeWithTag("sandbox_flow_home").performClick()
        compose.onNodeWithTag("sandbox_open_cia").performClick()
        // El MockHttpClient simula 1,2 s de latencia por pedido.
        compose.waitUntilAtLeastOneExists(hasTestTag("cia_home_merchant_name"), 15_000)
    }

    @Test
    fun el_lanzador_muestra_los_controles_principales() {
        compose.onNodeWithTag("sandbox_open_cia").assertIsDisplayed()
        compose.onNodeWithTag("sandbox_mocks").assertIsDisplayed()
        compose.onNodeWithTag("sandbox_tenant_individuos").assertIsDisplayed()
    }

    @Test
    fun iniciar_el_flujo_home_abre_la_home_con_el_comercio() {
        openHome()

        compose.onNodeWithTag("cia_home").assertIsDisplayed()
        compose.onNodeWithTag("cia_home_merchant_name").assertIsDisplayed()
    }

    @Test
    fun la_pantalla_de_mocks_se_abre_y_se_cierra() {
        compose.onNodeWithTag("sandbox_mocks").performClick()
        compose.onNodeWithTag("cia_mocks_screen").assertIsDisplayed()

        compose.onNodeWithTag("cia_mocks_done").performClick()

        compose.onNodeWithTag("sandbox_open_cia").assertIsDisplayed()
    }

    @Test
    fun todo_control_clickeable_de_la_home_tiene_etiqueta_para_accesibilidad() {
        openHome()

        val unlabeled = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .filter { node ->
                val description = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
                val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
                description.all { it.isBlank() } && text.all { it.text.isBlank() }
            }

        assertTrue(unlabeled.isEmpty(), "Controles clickeables sin etiqueta: ${unlabeled.map { it.id }}")
    }
}
