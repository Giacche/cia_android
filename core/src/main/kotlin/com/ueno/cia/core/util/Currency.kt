package com.ueno.cia.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/** Subconjunto de `Currency` del design system (solo guaraníes tiene uso real en CIA). */
data class CIACurrency(
    val code: String,
    val symbol: String,
    val decimalDigits: Int,
    val decimalSeparator: String,
    val thousandsSeparator: String,
) {
    companion object {
        val Gs = CIACurrency("GS", "Gs.", decimalDigits = 0, decimalSeparator = ",", thousandsSeparator = ".")

        /** `CurrencyHelper.getCurrency`: un código desconocido cae en guaraníes. */
        fun of(@Suppress("UNUSED_PARAMETER") code: String): CIACurrency = Gs
    }
}

/**
 * Equivale a `num.formatCurrency('GS')` de `ueno_app_core_dependencies`: `"Gs. 85.000"`.
 * El formato es fijo: no depende del idioma del dispositivo.
 */
class CurrencyFormatter(val currency: CIACurrency = CIACurrency.Gs) {
    fun string(amount: BigDecimal): String = "${currency.symbol} ${digits(amount)}"

    /** Solo el número con separadores, sin símbolo (lo usa el input de monto). */
    fun digits(amount: BigDecimal): String {
        // Dart `double.round()` redondea .5 hacia arriba (en positivos)
        val rounded = amount.setScale(currency.decimalDigits, RoundingMode.HALF_UP)
        val plain = rounded.abs().toPlainString()
        val integer = plain.substringBefore('.')
        val fraction = plain.substringAfter('.', "")
        val grouped = group(integer, currency.thousandsSeparator)
        val sign = if (rounded.signum() < 0) "-" else ""
        return sign + grouped + if (currency.decimalDigits > 0) currency.decimalSeparator + fraction else ""
    }

    companion object {
        val Gs = CurrencyFormatter(CIACurrency.Gs)

        /** Agrupa de a tres dígitos desde la derecha: `"1234567"` → `"1.234.567"`. */
        fun group(digits: String, separator: String): String =
            digits.reversed().chunked(3).joinToString(separator).reversed()
    }
}

/** Reemplazo de placeholders del BFF (`{amount}`, `{commission}`, `{commission_amount}`). */
object TemplateString {
    fun fill(template: String, values: Map<String, String>): String =
        values.entries.fold(template) { result, (key, value) -> result.replace("{$key}", value) }
}

/**
 * Los montos del BFF llegan como número JSON (`150000`, `5.5`). Se leen sin pasar por `Double` para no perder
 * precisión y se serializan como texto (lo leen igual al volver a decodificar, por ejemplo en los argumentos de ruta).
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val content = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive?.content ?: decoder.decodeString()
        return BigDecimal(content)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())
}
