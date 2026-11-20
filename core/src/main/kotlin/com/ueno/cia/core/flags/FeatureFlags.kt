package com.ueno.cia.core.flags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Una bandera con su valor por defecto: es lo que rige si ninguna fuente (remota o local) la define. */
data class FeatureFlag(val key: String, val default: Boolean = false)

/**
 * Puerto de feature flags. El host decide de dónde vienen los valores (Remote Config, un servicio propio, DataStore
 * para pruebas); la microapp solo pregunta. Así un cambio de proveedor no toca ninguna pantalla.
 */
interface FeatureFlags {
    fun isEnabled(flag: FeatureFlag): Boolean

    /** Emite el valor actual y cada cambio posterior. */
    fun observe(flag: FeatureFlag): Flow<Boolean>
}

/** Implementación en memoria: sirve como valor por defecto del host y como fake en tests. */
class InMemoryFeatureFlags(initial: Map<String, Boolean> = emptyMap()) : FeatureFlags {
    private val values = MutableStateFlow(initial)

    override fun isEnabled(flag: FeatureFlag): Boolean = values.value[flag.key] ?: flag.default

    override fun observe(flag: FeatureFlag): Flow<Boolean> = values.map { it[flag.key] ?: flag.default }

    fun set(key: String, enabled: Boolean) {
        values.value = values.value + (key to enabled)
    }

    fun clear(key: String) {
        values.value = values.value - key
    }
}
