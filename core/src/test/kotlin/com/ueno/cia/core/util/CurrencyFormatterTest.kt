package com.ueno.cia.core.util

import com.ueno.cia.core.network.CIAJson
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable

class CurrencyFormatterTest {
    private val formatter = CurrencyFormatter.Gs

    @Test
    fun `formatea guaranies con puntos de miles`() {
        assertEquals("Gs. 85.000", formatter.string(BigDecimal(85000)))
        assertEquals("Gs. 1.234.567", formatter.string(BigDecimal(1234567)))
        assertEquals("Gs. 0", formatter.string(BigDecimal.ZERO))
        assertEquals("Gs. 999", formatter.string(BigDecimal(999)))
    }

    @Test
    fun `redondea medio hacia arriba como Dart`() {
        assertEquals("Gs. 1.000", formatter.string(BigDecimal("999.5")))
        assertEquals("Gs. 8.250", formatter.string(BigDecimal("8249.5")))
        assertEquals("Gs. 99", formatter.string(BigDecimal("99.4999")))
    }

    @Test
    fun `agrupa solo los digitos`() {
        assertEquals("1.234", CurrencyFormatter.group("1234", "."))
        assertEquals("12", CurrencyFormatter.group("12", "."))
        assertEquals("123.456.789", CurrencyFormatter.group("123456789", "."))
    }

    @Test
    fun `completa las plantillas del BFF`() {
        assertEquals(
            "Comisión 5.5% (Gs. 825)",
            TemplateString.fill("Comisión {commission}% ({commission_amount})", mapOf("commission" to "5.5", "commission_amount" to "Gs. 825")),
        )
    }

    @Serializable
    private data class Box(@Serializable(with = BigDecimalSerializer::class) val value: BigDecimal)

    @Test
    fun `los montos del BFF se leen sin perder precision`() {
        assertEquals(BigDecimal("150000"), CIAJson.decodeFromString<Box>("""{"value":150000}""").value)
        assertEquals(BigDecimal("5.5"), CIAJson.decodeFromString<Box>("""{"value":5.5}""").value)
    }

    @Test
    fun `los montos sobreviven a una ida y vuelta por la ruta`() {
        val box = Box(BigDecimal("1234567.5"))
        assertEquals(box, CIAJson.decodeFromString<Box>(CIAJson.encodeToString(Box.serializer(), box)))
    }
}
