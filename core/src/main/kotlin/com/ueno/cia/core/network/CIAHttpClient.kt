package com.ueno.cia.core.network

import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.core.tenant.TenantContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

/**
 * Adapter de CIA sobre el [HttpClient] del host. Único lugar donde se agregan headers dinámicos por tenant,
 * se traducen los errores HTTP y se decodifica el envelope del BFF.
 */
class CIAHttpClient(
    private val base: HttpClient,
    private val tenantContext: TenantContext,
) {
    suspend inline fun <reified Response> get(
        path: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Response = get(serializer<Response>(), path, query, headers)

    suspend fun <Response> get(
        serializer: KSerializer<Response>,
        path: String,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): Response = perform(serializer, HttpRequest(HttpMethod.GET, path, query, headers))

    suspend inline fun <reified Body, reified Response> post(
        path: String,
        body: Body,
        headers: Map<String, String> = emptyMap(),
    ): Response = post(serializer<Body>(), serializer<Response>(), path, body, headers)

    suspend fun <Body, Response> post(
        bodySerializer: KSerializer<Body>,
        serializer: KSerializer<Response>,
        path: String,
        body: Body,
        headers: Map<String, String> = emptyMap(),
    ): Response = perform(
        serializer,
        HttpRequest(HttpMethod.POST, path, headers = headers, body = CIARequestJson.encodeToString(bodySerializer, body)),
    )

    suspend fun <Response> perform(serializer: KSerializer<Response>, request: HttpRequest): Response {
        val response = send(request)
        return try {
            CIAJson.decodeFromString(BFFEnvelope.serializer(serializer), response.body).data
        } catch (error: SerializationException) {
            throw APIError.Decoding(error.message ?: "Error de decodificación", error)
        } catch (error: IllegalArgumentException) {
            throw APIError.Decoding(error.message ?: "Error de decodificación", error)
        }
    }

    suspend fun send(request: HttpRequest): HttpResponse {
        // Los headers se calculan al enviar, con el tenant vigente en ese momento.
        val merged = request.copy(headers = mergedHeaders(request))
        val response = base.send(merged)
        if (response.statusCode !in 200..299) throw serverError(response)
        return response
    }

    /**
     * Flutter (`headersForRequest`): el origin del contexto es un default (el explícito del request gana);
     * la sucursal del contexto siempre pisa.
     */
    private fun mergedHeaders(request: HttpRequest): Map<String, String> {
        val snapshot = tenantContext.snapshot
        val headers = request.headers.toMutableMap()
        if (snapshot.tenant == Tenant.EMPRESAS && request.method == HttpMethod.GET && ORIGIN_HEADER !in headers) {
            headers[ORIGIN_HEADER] = ORIGIN_HOME_BUSINESS
        }
        snapshot.activeBranchId?.let { headers[BRANCH_ID_HEADER] = it }
        return headers
    }

    private fun serverError(response: HttpResponse): APIError.Server {
        val (code, message) = runCatching {
            val json = CIAJson.parseToJsonElement(response.body)
            val root = (json as? kotlinx.serialization.json.JsonObject) ?: return@runCatching null to null
            val error = (root["error"] as? kotlinx.serialization.json.JsonObject) ?: root
            fun field(name: String) = (error[name] as? kotlinx.serialization.json.JsonPrimitive)?.content
            field("code") to field("message")
        }.getOrDefault(null to null)
        return APIError.Server(response.statusCode, code, message)
    }

    companion object {
        const val BRANCH_ID_HEADER = "x-branch-id"
        const val ORIGIN_HEADER = "x-origin"
        const val ORIGIN_HOME_BUSINESS = "HOME_BUSINESS"
    }
}
