package com.ueno.cia.core.security

/**
 * Almacén de secretos (tokens, claves de sesión). La implementación de Android cifra con una clave que vive en el
 * Android Keystore y nunca sale del hardware; `core` solo conoce el contrato.
 */
interface SecureStorage {
    suspend fun put(key: String, value: String)

    suspend fun get(key: String): String?

    suspend fun remove(key: String)

    suspend fun clear()
}

/** Fake en memoria para tests de quien consume el contrato. No cifra nada. */
class InMemorySecureStorage : SecureStorage {
    private val data = mutableMapOf<String, String>()

    override suspend fun put(key: String, value: String) {
        data[key] = value
    }

    override suspend fun get(key: String): String? = data[key]

    override suspend fun remove(key: String) {
        data.remove(key)
    }

    override suspend fun clear() = data.clear()
}
