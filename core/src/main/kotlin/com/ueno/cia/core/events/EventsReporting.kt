package com.ueno.cia.core.events

import com.ueno.cia.core.network.APIError
import kotlin.coroutines.cancellation.CancellationException

/** Telemetría de errores (`POST /bff/smartcharge/v1/events` en el BFF). Nunca debe romper el flujo. */
data class RepositoryError(val type: String, val message: String, val code: String = "Unknown")

interface EventsReporting {
    suspend fun report(type: String, error: Throwable, path: String)

    /** Aviso puntual al BFF (por ejemplo una acción del usuario). */
    suspend fun notify(error: RepositoryError)
}

object NoOpEvents : EventsReporting {
    override suspend fun report(type: String, error: Throwable, path: String) = Unit
    override suspend fun notify(error: RepositoryError) = Unit
}

/** Equivale a `reportingError` de Flutter: reporta el error y lo vuelve a lanzar. */
suspend inline fun <T> EventsReporting.reportingError(type: String, path: String, block: () -> T): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: APIError) {
    runCatching { report(type, error, path) }
    throw error
}
