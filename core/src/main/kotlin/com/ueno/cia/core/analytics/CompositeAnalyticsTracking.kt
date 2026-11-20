package com.ueno.cia.core.analytics

/**
 * Envía cada evento a varios destinos (por ejemplo Amplitude y Firebase) detrás del mismo puerto [AnalyticsTracking].
 *
 * Una falla en un destino **no** debe romper a los demás ni a la pantalla que emitió el evento: se captura y se
 * informa a [onError]. La analítica nunca puede ser la causa de un crash.
 */
class CompositeAnalyticsTracking(
    private val trackers: List<AnalyticsTracking>,
    private val onError: (tracker: AnalyticsTracking, error: Throwable) -> Unit = { _, _ -> },
) : AnalyticsTracking {
    constructor(vararg trackers: AnalyticsTracking) : this(trackers.toList())

    override fun track(event: AnalyticsEvent) {
        trackers.forEach { tracker ->
            runCatching { tracker.track(event) }.onFailure { onError(tracker, it) }
        }
    }
}
