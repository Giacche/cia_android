package com.ueno.cia.storage

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class EncryptedSecureStorageTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun newCipher(): AesGcmCipher {
        val key = newKey()
        return AesGcmCipher { key }
    }

    @Test
    fun `guarda y devuelve el valor`() = runTest(UnconfinedTestDispatcher()) {
        val storage = EncryptedSecureStorage(testDataStore(backgroundScope, "a"), newCipher())

        storage.put("token", "abc")

        assertEquals("abc", storage.get("token"))
    }

    @Test
    fun `en disco el valor esta cifrado`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = testDataStore(backgroundScope, "b")
        val storage = EncryptedSecureStorage(dataStore, newCipher())

        storage.put("token", "abc")

        val raw = dataStore.data.first()[stringPreferencesKey("secure_token")]
        assertNotEquals("abc", raw)
        assertEquals(false, raw.orEmpty().contains("abc"))
    }

    @Test
    fun `remove y clear borran los valores`() = runTest(UnconfinedTestDispatcher()) {
        val storage = EncryptedSecureStorage(testDataStore(backgroundScope, "c"), newCipher())
        storage.put("a", "1")
        storage.put("b", "2")

        storage.remove("a")
        assertNull(storage.get("a"))
        assertEquals("2", storage.get("b"))

        storage.clear()
        assertNull(storage.get("b"))
    }

    @Test
    fun `si la clave cambio el valor se trata como inexistente y se borra`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = testDataStore(backgroundScope, "d")
        val oldKey = newKey()
        EncryptedSecureStorage(dataStore, AesGcmCipher { oldKey }).put("token", "abc")

        val withNewKey = EncryptedSecureStorage(dataStore, newCipher())

        assertNull(withNewKey.get("token"))
        assertNull(dataStore.data.first()[stringPreferencesKey("secure_token")])
    }

    @Test
    fun `un valor corrupto en disco no rompe la lectura`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = testDataStore(backgroundScope, "e")
        val storage = EncryptedSecureStorage(dataStore, newCipher())
        storage.put("token", "abc")
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().also { it[stringPreferencesKey("secure_token")] = "###no-es-base64###" }
        }

        assertNull(storage.get("token"))
    }
}
