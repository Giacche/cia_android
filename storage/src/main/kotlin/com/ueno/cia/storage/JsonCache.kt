package com.ueno.cia.storage

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Una respuesta guardada: la clave identifica la consulta (por ejemplo `movements:page=1`). */
@Entity(tableName = "cache_entries")
data class CacheEntryEntity(
    @PrimaryKey val key: String,
    val json: String,
    @ColumnInfo(name = "saved_at") val savedAtMillis: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entries WHERE `key` = :key")
    suspend fun find(key: String): CacheEntryEntity?

    @Upsert
    suspend fun upsert(entry: CacheEntryEntity)

    @Query("DELETE FROM cache_entries WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM cache_entries WHERE saved_at < :olderThanMillis")
    suspend fun deleteOlderThan(olderThanMillis: Long): Int

    @Query("DELETE FROM cache_entries")
    suspend fun clear()
}

@Database(entities = [CacheEntryEntity::class], version = 1, exportSchema = true)
abstract class CIADatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}

/**
 * Caché de respuestas del BFF con vencimiento. Permite mostrar lo último conocido cuando no hay conexión.
 * Guarda JSON crudo, así que no depende de los modelos de cada feature ni obliga a migrar la base cuando cambian.
 *
 * No guardar datos sensibles acá: la base no está cifrada (para secretos, [EncryptedSecureStorage]).
 */
class JsonCache(
    private val dao: CacheDao,
    private val ttl: Duration = 15.minutes,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Devuelve el JSON si existe y no venció; si venció lo borra y devuelve `null`. */
    suspend fun get(key: String): String? {
        val entry = dao.find(key) ?: return null
        if (now() - entry.savedAtMillis > ttl.inWholeMilliseconds) {
            dao.delete(key)
            return null
        }
        return entry.json
    }

    suspend fun put(key: String, json: String) = dao.upsert(CacheEntryEntity(key, json, now()))

    suspend fun invalidate(key: String) = dao.delete(key)

    /** Limpieza periódica de lo vencido. Devuelve cuántas entradas borró. */
    suspend fun purgeExpired(): Int = dao.deleteOlderThan(now() - ttl.inWholeMilliseconds)

    suspend fun clear() = dao.clear()
}
