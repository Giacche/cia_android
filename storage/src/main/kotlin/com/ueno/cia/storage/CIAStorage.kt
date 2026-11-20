package com.ueno.cia.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ueno.cia.core.security.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val Context.ciaDataStore: DataStore<Preferences> by preferencesDataStore(name = "cia_storage")

/**
 * Punto de entrada para que el host arme lo que necesita de `:storage`. Cada función usa el contexto de aplicación,
 * así que se puede llamar desde una `Activity` sin filtrar referencias.
 */
object CIAStorage {
    @Volatile private var database: CIADatabase? = null

    /** Feature flags persistidos. [scope] debe vivir lo mismo que la app (por ejemplo, el de la `Application`). */
    fun featureFlags(context: Context, scope: CoroutineScope): DataStoreFeatureFlags =
        DataStoreFeatureFlags(context.applicationContext.ciaDataStore, scope)

    /** Almacén de secretos cifrado con una clave del Android Keystore. */
    fun secureStorage(context: Context, keyAlias: String = DEFAULT_KEY_ALIAS): SecureStorage {
        val keys = AndroidKeystoreKeyProvider(keyAlias)
        return EncryptedSecureStorage(context.applicationContext.ciaDataStore, AesGcmCipher(keys::get))
    }

    /** Caché de respuestas con vencimiento sobre Room. La base se crea una sola vez. */
    fun jsonCache(context: Context, ttl: Duration = 15.minutes): JsonCache = JsonCache(database(context).cacheDao(), ttl)

    private fun database(context: Context): CIADatabase = database ?: synchronized(this) {
        database ?: Room.databaseBuilder(context.applicationContext, CIADatabase::class.java, "cia.db")
            .build()
            .also { database = it }
    }

    private const val DEFAULT_KEY_ALIAS = "cia_secure_key"
}
