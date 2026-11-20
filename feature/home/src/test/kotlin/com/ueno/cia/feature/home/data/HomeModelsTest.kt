package com.ueno.cia.feature.home.data

import com.ueno.cia.core.model.HomeOptionKind
import com.ueno.cia.core.model.MovementsResponse
import com.ueno.cia.feature.home.Contratos
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeModelsTest {
    private fun serializerFor(name: String): KSerializer<*> = when {
        name.startsWith("home_get_home_layout") -> HomeLayoutResponse.serializer()
        name.startsWith("home_get_home_header") -> HomeHeaderResponse.serializer()
        name.startsWith("home_get_home_options") -> HomeOptionsResponse.serializer()
        name.startsWith("home_get_home_banners") -> HomeBannersResponse.serializer()
        name.startsWith("home_get_home_recents") -> HomeRecentsResponse.serializer()
        name.contains("alerts") -> HomeAlertsResponse.serializer()
        name.contains("pending_settlement") -> HomePendingSettlementResponse.serializer()
        else -> error("Contrato sin modelo: $name")
    }

    @Test
    fun `todos los contratos de la home se decodifican`() {
        val names = Contratos.names("home")
        assertTrue(names.isNotEmpty())
        names.forEach { name ->
            runCatching { Contratos.decode(serializerFor(name), "home", name) }
                .onFailure { throw AssertionError("No se pudo decodificar $name", it) }
        }
    }

    @Test
    fun `el contrato de cobros se decodifica como MovementsResponse`() {
        val response = Contratos.decode(MovementsResponse.serializer(), "charges", "bff_charges_tabs_cobros_get_response.json")
        assertTrue(response.charges.isNotEmpty())
    }

    @Test
    fun `el layout por defecto declara sus secciones en orden`() {
        val layout = Contratos.decode(HomeLayoutResponse.serializer(), "home", "home_get_home_layout.json").layout
        assertEquals(
            listOf("header", "alerts", "options", "banners", "recents"),
            layout.sections.map { it.id }.take(5),
        )
        assertNull(layout.errorScreen)
    }

    @Test
    fun `el layout de error de Empresas trae errorScreen`() {
        val layout = Contratos.decode(HomeLayoutResponse.serializer(), "home", "home_get_home_layout_error_empresas.json").layout
        assertNotNull(layout.errorScreen)
    }

    @Test
    fun `un id de opcion desconocido se interpreta como QR Ueno`() {
        assertEquals(HomeOptionKind.QrUeno, HomeOptionKind.fromId("ALGO_NUEVO"))
        assertEquals(HomeOptionKind.Ttp, HomeOptionKind.fromId("TTP"))
    }

    @Test
    fun `un id de seccion desconocido se ignora`() {
        assertNull(HomeSectionID.fromId("seccion_nueva"))
        assertTrue(HomeSectionID.fromId("total_sales")!!.isPendingSettlement)
        assertTrue(HomeSectionID.fromId("pending_settlement")!!.isPendingSettlement)
        assertTrue(HomeSectionID.fromId("header_carousel")!!.isHeader)
        assertFalse(HomeSectionID.fromId("options")!!.isHeader)
    }

    @Test
    fun `el total de ventas acepta totalSales y pendingSettlement`() {
        val a = com.ueno.cia.core.network.CIAJson.decodeFromString(
            HomePendingSettlementResponse.serializer(),
            """{"type":"X","totalSales":{"title":"A","amount":"1"}}""",
        )
        val b = com.ueno.cia.core.network.CIAJson.decodeFromString(
            HomePendingSettlementResponse.serializer(),
            """{"type":"X","pendingSettlement":{"title":"B","amount":"2"}}""",
        )
        assertEquals("A", a.value.title)
        assertEquals("B", b.value.title)
    }

    @Test
    fun `las opciones sin disabled quedan habilitadas`() {
        val options = Contratos.decode(HomeOptionsResponse.serializer(), "home", "home_get_home_options.json")
        assertTrue(options.options.isNotEmpty())
        assertTrue(options.options.all { !it.disabled || it.disabled })
    }
}
