package com.ueno.cia.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ueno.cia.core.security.SecureStorage
import kotlinx.coroutines.flow.first
import java.security.GeneralSecurityException
import java.util.Base64

/**
 * [SecureStorage] que guarda los valores **cifrados** en DataStore. El texto plano nunca toca el disco.
 *
 * Si un valor no se puede descifrar (la clave del Keystore se perdió, por ejemplo al restaurar un backup en otro
 * dispositivo, o el dato fue alterado) se trata como inexistente y se borra: es preferible pedir iniciar sesión de
 * nuevo que fallar o devolver basura.
 */
class EncryptedSecureStorage(
    private val dataStore: DataStore<Preferences>,
    private val cipher: AesGcmCipher,
) : SecureStorage {
    override suspend fun put(key: String, value: String) {
        val encrypted = Base64.getEncoder().encodeToString(cipher.encrypt(value.encodeToByteArray()))
        dataStore.edit { it[prefKey(key)] = encrypted }
    }

    override suspend fun get(key: String): String? {
        val stored = dataStore.data.first()[prefKey(key)] ?: return null
        return try {
            cipher.decrypt(Base64.getDecoder().decode(stored)).decodeToString()
        } catch (_: GeneralSecurityException) {
            remove(key)
            null
        } catch (_: IllegalArgumentException) {
            remove(key)
            null
        }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(prefKey(key)) }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { prefs.remove(it) } }
    }

    private fun prefKey(key: String) = stringPreferencesKey("$PREFIX$key")

    private companion object {
        const val PREFIX = "secure_"
    }
}
