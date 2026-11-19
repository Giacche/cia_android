package com.ueno.cia.feature.paymentselector

import com.ueno.cia.core.network.BFFEnvelope
import com.ueno.cia.core.network.CIAJson
import java.io.File
import kotlinx.serialization.KSerializer

/** Lee los mismos fixtures JSON del BFF que usa el sandbox (`app/src/main/assets/fixtures`). */
object Fixtures {
    private val root = listOf("../../app/src/main/assets/fixtures", "app/src/main/assets/fixtures")
        .map(::File)
        .first { it.isDirectory }

    fun <T> decode(serializer: KSerializer<T>, folder: String, name: String): T =
        CIAJson.decodeFromString(BFFEnvelope.serializer(serializer), File(root, "$folder/$name").readText()).data
}
