package com.ueno.cia.feature.home

import com.ueno.cia.core.network.BFFEnvelope
import com.ueno.cia.core.network.CIAJson
import java.io.File
import kotlinx.serialization.KSerializer

/** Lee los mismos fixtures JSON del BFF que usa el sandbox (`app/src/main/assets/fixtures`). */
object Fixtures {
    private val root = listOf("../../app/src/main/assets/fixtures", "app/src/main/assets/fixtures")
        .map(::File)
        .first { it.isDirectory }

    fun text(folder: String, name: String): String = File(root, "$folder/$name").readText()

    fun <T> decode(serializer: KSerializer<T>, folder: String, name: String): T =
        CIAJson.decodeFromString(BFFEnvelope.serializer(serializer), text(folder, name)).data

    fun names(folder: String): List<String> =
        File(root, folder).listFiles { file -> file.extension == "json" }!!.map { it.name }.sorted()
}
