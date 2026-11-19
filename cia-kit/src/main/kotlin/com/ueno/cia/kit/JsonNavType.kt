package com.ueno.cia.kit

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.ueno.cia.core.network.CIAJson
import kotlinx.serialization.encodeToString

/**
 * Navigation Compose solo sabe pasar primitivos y enums; las rutas con modelos del BFF (`Movements`, `Congrats`...)
 * los viajan como JSON dentro del argumento.
 */
internal inline fun <reified T : Any> jsonNavType(): NavType<T> = object : NavType<T>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): T? =
        bundle.getString(key)?.let { CIAJson.decodeFromString<T>(it) }

    // Navigation ya decodifica el argumento de la ruta: volver a decodificar corrompe los textos con "%" (ej. "2.9% + IVA")
    override fun parseValue(value: String): T = CIAJson.decodeFromString<T>(value)

    // La ruta sí necesita el valor codificado
    override fun serializeAsValue(value: T): String = Uri.encode(CIAJson.encodeToString(value))

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, CIAJson.encodeToString(value))
    }
}
