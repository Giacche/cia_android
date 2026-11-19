package com.ueno.cia.core.network

enum class HttpMethod { GET, POST, PUT }

data class HttpRequest(
    val method: HttpMethod,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

data class HttpResponse(val statusCode: Int, val body: String)

/**
 * Contrato con el host (equivale a `IMultitenantHttpClient`). Devuelve la respuesta cruda; los status no 2xx
 * los traduce [CIAHttpClient] a [APIError.Server]. Las fallas de red se lanzan como [APIError.Connectivity].
 */
interface HttpClient {
    suspend fun send(request: HttpRequest): HttpResponse
}
