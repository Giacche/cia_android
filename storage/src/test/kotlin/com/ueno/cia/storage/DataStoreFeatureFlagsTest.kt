package com.ueno.cia.storage

import app.cash.turbine.test
import com.ueno.cia.core.flags.FeatureFlag
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreFeatureFlagsTest {
    private val flag = FeatureFlag("qr_nuevo", default = false)

    @Test
    fun `sin valor rige el default y set lo persiste`() = runTest(UnconfinedTestDispatcher()) {
        val flags = DataStoreFeatureFlags(testDataStore(backgroundScope, "f1"), backgroundScope)
        assertEquals(false, flags.isEnabled(flag))

        flags.set("qr_nuevo", true)

        assertEquals(true, flags.isEnabled(flag))
    }

    @Test
    fun `clear restituye el default`() = runTest(UnconfinedTestDispatcher()) {
        val flags = DataStoreFeatureFlags(testDataStore(backgroundScope, "f2"), backgroundScope)
        flags.set("qr_nuevo", true)

        flags.clear("qr_nuevo")

        assertEquals(false, flags.isEnabled(flag))
    }

    @Test
    fun `observe emite los cambios sin repetir valores`() = runTest(UnconfinedTestDispatcher()) {
        val flags = DataStoreFeatureFlags(testDataStore(backgroundScope, "f3"), backgroundScope)

        flags.observe(flag).test {
            assertEquals(false, awaitItem())
            flags.set("qr_nuevo", true)
            assertEquals(true, awaitItem())
            flags.set("qr_nuevo", true)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
