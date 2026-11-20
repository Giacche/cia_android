package com.ueno.cia.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ueno.cia.core.flags.FeatureFlag
import com.ueno.cia.core.flags.FeatureFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Feature flags persistidos en DataStore. Sirve como **override local** (pantalla de debug, QA) o como caché del
 * último valor remoto; el proveedor remoto real se integra detrás del mismo puerto [FeatureFlags].
 *
 * [isEnabled] es síncrono (se lee desde composición o desde un ViewModel) y por eso se apoya en una copia en memoria
 * que se mantiene al día con DataStore. Hasta que carga la primera vez rige el default de la bandera.
 */
class DataStoreFeatureFlags(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) : FeatureFlags {
    private val cache: StateFlow<Preferences> =
        dataStore.data.stateIn(scope, SharingStarted.Eagerly, androidx.datastore.preferences.core.emptyPreferences())

    override fun isEnabled(flag: FeatureFlag): Boolean = cache.value[key(flag.key)] ?: flag.default

    override fun observe(flag: FeatureFlag): Flow<Boolean> = dataStore.data.map { it[key(flag.key)] ?: flag.default }.distinctUntilChanged()

    suspend fun set(flagKey: String, enabled: Boolean) {
        dataStore.edit { it[key(flagKey)] = enabled }
    }

    suspend fun clear(flagKey: String) {
        dataStore.edit { it.remove(key(flagKey)) }
    }

    private fun key(name: String) = booleanPreferencesKey("flag_$name")
}
