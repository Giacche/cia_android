package com.ueno.cia.core.network

sealed class APIError(message: String?, cause: Throwable? = null) : Exception(message, cause) {
    /** Error devuelto por el BFF (status no 2xx). `code` es el código de negocio si viene. */
    class Server(val statusCode: Int, val code: String?, val serverMessage: String?) :
        APIError("HTTP $statusCode ${code.orEmpty()} ${serverMessage.orEmpty()}".trim())

    class Connectivity(cause: Throwable? = null) : APIError("Sin conexión", cause)

    class Decoding(detail: String, cause: Throwable? = null) : APIError(detail, cause)

    class Unknown(detail: String, cause: Throwable? = null) : APIError(detail, cause)
}
