package com.ueno.cia.core.network

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Implementación de [HttpClient] sobre OkHttp, para el sandbox contra un BFF real o cuando el host no provee cliente. */
class OkHttpHttpClient(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
) : HttpClient {
    override suspend fun send(request: HttpRequest): HttpResponse {
        val url = baseUrl.trimEnd('/').plus(request.path).toHttpUrl().newBuilder().apply {
            request.query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val body = request.body?.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder().url(url)
        request.headers.forEach { (key, value) -> builder.header(key, value) }
        when (request.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> builder.post(body ?: "".toRequestBody(JSON_MEDIA_TYPE))
            HttpMethod.PUT -> builder.put(body ?: "".toRequestBody(JSON_MEDIA_TYPE))
        }
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(builder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(APIError.Connectivity(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        continuation.resume(HttpResponse(it.code, it.body.string()))
                    }
                }
            })
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
