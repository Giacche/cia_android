package com.ueno.cia.core.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeAnalyticsTrackingTest {
    private val event =
        AnalyticsEvent(
            kind = AnalyticsEvent.Kind.BUTTON_TAP,
            squadName = "squad",
            flow = "flow",
            subFlow = "sub",
            parameters = emptyMap(),
            extraParams = emptyMap(),
        )

    @Test
    fun `cada destino recibe el evento`() {
        val a = mockk<AnalyticsTracking>(relaxed = true)
        val b = mockk<AnalyticsTracking>(relaxed = true)

        CompositeAnalyticsTracking(a, b).track(event)

        verify(exactly = 1) { a.track(event) }
        verify(exactly = 1) { b.track(event) }
    }

    @Test
    fun `la falla de un destino no impide que los demas reciban el evento`() {
        val broken = mockk<AnalyticsTracking>()
        every { broken.track(any()) } throws IllegalStateException("sin red")
        val healthy = mockk<AnalyticsTracking>(relaxed = true)
        val errors = mutableListOf<Throwable>()

        CompositeAnalyticsTracking(listOf(broken, healthy)) { _, error -> errors += error }.track(event)

        verify(exactly = 1) { healthy.track(event) }
        assertEquals(1, errors.size)
    }

    @Test
    fun `sin destinos no hace nada`() {
        CompositeAnalyticsTracking(emptyList()).track(event)
    }
}
