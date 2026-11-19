package com.ueno.cia.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Las respuestas del BFF vienen envueltas en `{ "data": { ... } }`. */
@Serializable
data class BFFEnvelope<T>(val data: T)

/** Decoder compartido: campos nuevos del BFF se ignoran, igual que `json_serializable` en Flutter. */
val CIAJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

/** Para los cuerpos de request: Flutter manda `documentNumber: null` explícito cuando no hay CPF. */
val CIARequestJson: Json = Json {
    explicitNulls = true
    encodeDefaults = true
}
