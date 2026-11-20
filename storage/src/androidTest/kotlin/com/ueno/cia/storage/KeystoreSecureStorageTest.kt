package com.ueno.cia.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** Usa el Android Keystore **real** del dispositivo: es lo único que los tests de JVM no pueden verificar. */
@RunWith(AndroidJUnit4::class)
class KeystoreSecureStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val alias = "cia_test_${UUID.randomUUID()}"
    private val keys = AndroidKeystoreKeyProvider(alias)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File

    private fun storage(): EncryptedSecureStorage {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        return EncryptedSecureStorage(dataStore, AesGcmCipher(keys::get))
    }

    @Before
    fun setUp() {
        file = File(context.cacheDir, "keystore_test_${UUID.randomUUID()}.preferences_pb")
    }

    @After
    fun tearDown() {
        keys.delete()
        scope.cancel()
        file.delete()
    }

    @Test
    fun la_clave_se_crea_una_vez_y_se_reutiliza() {
        val first = keys.get()
        val second = keys.get()

        assertEquals(first.algorithm, "AES")
        assertEquals(first.encoded == null, true, "El Keystore no debe exponer el material de la clave")
        assertEquals(first, second)
    }

    @Test
    fun guarda_y_recupera_un_secreto_con_la_clave_del_keystore() = runBlocking {
        val storage = storage()

        storage.put("token", "secreto-123")

        assertEquals("secreto-123", storage.get("token"))
    }

    @Test
    fun el_secreto_no_queda_en_texto_plano_en_el_disco() = runBlocking {
        storage().put("token", "secreto-123")

        val onDisk = file.readBytes().decodeToString(throwOnInvalidSequence = false)
        assertEquals(false, onDisk.contains("secreto-123"))
    }

    @Test
    fun si_se_borra_la_clave_el_secreto_queda_ilegible_y_se_descarta() = runBlocking {
        val storage = storage()
        storage.put("token", "secreto-123")

        keys.delete()

        assertNull(storage.get("token"))
        assertNotEquals("secreto-123", storage.get("token"))
    }
}
