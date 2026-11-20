package com.ueno.cia.core.model

import com.ueno.cia.core.network.CIAJson
import com.ueno.cia.core.util.BigDecimalSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.math.BigDecimal
import java.time.Instant

// features/detail/screens/receipt/models (Flutter).

/** El BFF manda `2023-10-01T00:00:00.000Z` (Dart `DateTime.parse`). */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
}

@Serializable
data class ReceiptScreenModel(
    val tag: String,
    val receiptName: String,
    val appBarTitle: String,
    val subTitle: String,
    val downloadLabel: String,
    val shareLabel: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val currency: String,
    @Serializable(with = InstantSerializer::class) val date: Instant,
    val items: List<ReceiptItemModel>,
)

@Serializable
data class ReceiptItemModel(
    val type: String,
    val value: String? = null,
    val subtitle: String? = null,
    val enabled: Boolean? = null,
    val light: Boolean? = null,
)

/** `POST …/confirm`: pantalla de resultado y comprobante. Un objeto vacío (`{}`) significa "todavía no hay resultado". */
data class CongratsConfirmResponse(
    val responseScreen: CongratsScreenModel?,
    val receiptScreen: ReceiptScreenModel?,
) {
    val hasResponse: Boolean get() = responseScreen != null
}

/** Los objetos vacíos (`{}`) del confirm significan "todavía no hay resultado": se leen como [JsonObject]. */
@Serializable
data class CongratsConfirmDto(
    val responseScreen: JsonObject? = null,
    val receiptScreen: JsonObject? = null,
) {
    /** Flutter: `map != null && map.isNotEmpty ? Model.fromJson(map) : null`. */
    fun toResponse() = CongratsConfirmResponse(
        responseScreen = responseScreen?.takeIf { it.isNotEmpty() }?.let { CIAJson.decodeFromJsonElement<CongratsScreenModel>(it) },
        receiptScreen = receiptScreen?.takeIf { it.isNotEmpty() }?.let { CIAJson.decodeFromJsonElement<ReceiptScreenModel>(it) },
    )
}
