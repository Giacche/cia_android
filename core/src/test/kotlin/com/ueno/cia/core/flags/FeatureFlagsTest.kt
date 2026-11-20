package com.ueno.cia.core.flags

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsTest {
    private val flag = FeatureFlag("qr_nuevo", default = false)

    @Test
    fun `sin valor definido rige el default de la bandera`() {
        assertFalse(InMemoryFeatureFlags().isEnabled(flag))
        assertTrue(InMemoryFeatureFlags().isEnabled(flag.copy(default = true)))
    }

    @Test
    fun `un valor definido pisa el default y clear lo restituye`() {
        val flags = InMemoryFeatureFlags()

        flags.set("qr_nuevo", true)
        assertTrue(flags.isEnabled(flag))

        flags.clear("qr_nuevo")
        assertFalse(flags.isEnabled(flag))
    }

    @Test
    fun `observe emite el valor actual y cada cambio`() = runTest {
        val flags = InMemoryFeatureFlags()

        flags.observe(flag).test {
            assertEquals(false, awaitItem())
            flags.set("qr_nuevo", true)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
