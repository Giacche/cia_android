package com.ueno.cia.sandbox

import android.content.Context
import com.ueno.cia.core.network.APIError
import com.ueno.cia.core.network.HttpClient
import com.ueno.cia.core.network.HttpRequest
import com.ueno.cia.core.network.HttpResponse
import kotlinx.coroutines.delay

/** Respuesta elegida por endpoint, persistida entre ejecuciones. */
class MockSelectionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("cia_mocks", Context.MODE_PRIVATE)

    fun selectedIndex(endpoint: MockEndpoint): Int {
        val index = prefs.getInt(PREFIX + endpoint.key, 0)
        return if (index in endpoint.responses.indices) index else 0
    }

    fun select(index: Int, endpoint: MockEndpoint) {
        prefs.edit().putInt(PREFIX + endpoint.key, index).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFIX = "cia.mock."
    }
}

/**
 * Equivale a `smart_charge_mobile_dev_tools` (`MockHttpClientProxy`): responde con contratos del BFF en lugar de
 * pegarle a la red. Es el `HttpClient` que el sandbox le provee a CIA.
 */
class MockHttpClient(
    context: Context,
    private val flows: List<MockFlow>,
    private val selection: MockSelectionStore,
    private val latencyMillis: Long = 1200,
) : HttpClient {
    private val assets = context.applicationContext.assets

    /** nombre de archivo → ruta dentro de `assets/contratos`. */
    private val contratos: Map<String, String> = buildMap {
        assets.list(CONTRATOS_DIR).orEmpty().forEach { folder ->
            assets.list("$CONTRATOS_DIR/$folder").orEmpty().forEach { file -> put(file, "$CONTRATOS_DIR/$folder/$file") }
        }
    }

    override suspend fun send(request: HttpRequest): HttpResponse {
        delay(latencyMillis)
        val endpoint = flows.asSequence()
            .flatMap { it.endpoints }
            .firstOrNull { it.matches(request.method, request.path, request.query + request.headers) }
            ?: return error(404, "Sin mock para ${request.method} ${request.path}")
        return when (val response = endpoint.responses[selection.selectedIndex(endpoint)]) {
            is MockResponse.Success -> HttpResponse(200, readContrato(response.contrato))
            is MockResponse.Json -> HttpResponse(200, response.body)
            is MockResponse.ApiError -> error(response.statusCode, response.description)
        }
    }

    private fun readContrato(name: String): String {
        val path = contratos[name] ?: throw APIError.Unknown("Falta el contrato $name")
        return assets.open(path).bufferedReader().use { it.readText() }
    }

    private fun error(status: Int, message: String) = HttpResponse(status, """{"error":{"code":"MOCK_$status","message":"$message"}}""")

    private companion object {
        const val CONTRATOS_DIR = "contratos"
    }
}
