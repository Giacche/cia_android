package com.ueno.cia.storage

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/** Ejercita Room de verdad (SQLite del dispositivo) con una base en memoria. */
@RunWith(AndroidJUnit4::class)
class JsonCacheRoomTest {
    private lateinit var db: CIADatabase
    private var clock = 0L
    private lateinit var cache: JsonCache

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, CIADatabase::class.java).allowMainThreadQueries().build()
        cache = JsonCache(db.cacheDao(), ttl = 10.minutes) { clock }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun guarda_y_lee_en_sqlite() = runBlocking {
        cache.put("movements:page=1", """{"items":[1,2,3]}""")

        assertEquals("""{"items":[1,2,3]}""", cache.get("movements:page=1"))
    }

    @Test
    fun el_vencimiento_borra_la_fila() = runBlocking {
        cache.put("k", "{}")
        clock += 11.minutes.inWholeMilliseconds

        assertNull(cache.get("k"))
        assertNull(db.cacheDao().find("k"))
    }

    @Test
    fun upsert_reemplaza_el_valor_de_la_misma_clave() = runBlocking {
        cache.put("k", "viejo")
        cache.put("k", "nuevo")

        assertEquals("nuevo", cache.get("k"))
    }

    @Test
    fun purgeExpired_borra_solo_lo_vencido() = runBlocking {
        cache.put("viejo", "1")
        clock += 11.minutes.inWholeMilliseconds
        cache.put("nuevo", "2")

        assertEquals(1, cache.purgeExpired())
        assertEquals("2", cache.get("nuevo"))
    }
}
