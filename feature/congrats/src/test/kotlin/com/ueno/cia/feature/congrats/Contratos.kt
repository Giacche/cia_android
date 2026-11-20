package com.ueno.cia.feature.congrats

import com.ueno.cia.core.network.BFFEnvelope
import com.ueno.cia.core.network.CIAJson
import kotlinx.serialization.KSerializer
import java.io.File

/** Lee los mismos contratos JSON del BFF que usa el sandbox (`app/src/main/assets/contratos`). */
object Contratos {
    private val root = listOf("../../app/src/main/assets/contratos", "app/src/main/assets/contratos")
        .map(::File)
        .first { it.isDirectory }

    fun <T> decode(serializer: KSerializer<T>, folder: String, name: String): T =
        CIAJson.decodeFromString(BFFEnvelope.serializer(serializer), File(root, "$folder/$name").readText()).data
}
