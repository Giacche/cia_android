package com.ueno.cia.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

private class FakeCacheDao : CacheDao {
    val entries = mutableMapOf<String, CacheEntryEntity>()

    override suspend fun find(key: String) = entries[key]

    override suspend fun upsert(entry: CacheEntryEntity) {
        entries[entry.key] = entry
    }

    override suspend fun delete(key: String) {
        entries.remove(key)
    }

    override suspend fun deleteOlderThan(olderThanMillis: Long): Int {
        val old = entries.filterValues { it.savedAtMillis < olderThanMillis }.keys
        old.forEach(entries::remove)
        return old.size
    }

    override suspend fun clear() = entries.clear()
}

class JsonCacheTest {
    private var clock = 0L
    private val dao = FakeCacheDao()
    private val cache = JsonCache(dao, ttl = 10.minutes) { clock }

    @Test
    fun `devuelve lo guardado mientras no venza`() = runTest {
        cache.put("k", "{}")
        clock += 9.minutes.inWholeMilliseconds

        assertEquals("{}", cache.get("k"))
    }

    @Test
    fun `lo vencido no se devuelve y se borra`() = runTest {
        cache.put("k", "{}")
        clock += 11.minutes.inWholeMilliseconds

        assertNull(cache.get("k"))
        assertNull(dao.entries["k"])
    }

    @Test
    fun `guardar de nuevo reemplaza el valor y renueva el vencimiento`() = runTest {
        cache.put("k", "viejo")
        clock += 9.minutes.inWholeMilliseconds
        cache.put("k", "nuevo")
        clock += 9.minutes.inWholeMilliseconds

        assertEquals("nuevo", cache.get("k"))
    }

    @Test
    fun `purgeExpired borra solo lo vencido`() = runTest {
        cache.put("viejo", "1")
        clock += 11.minutes.inWholeMilliseconds
        cache.put("nuevo", "2")

        assertEquals(1, cache.purgeExpired())
        assertEquals("2", cache.get("nuevo"))
    }

    @Test
    fun `invalidate y clear`() = runTest {
        cache.put("a", "1")
        cache.put("b", "2")

        cache.invalidate("a")
        assertNull(cache.get("a"))

        cache.clear()
        assertNull(cache.get("b"))
    }
}
