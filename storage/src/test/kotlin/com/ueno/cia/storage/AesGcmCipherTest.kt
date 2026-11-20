package com.ueno.cia.storage

import java.security.GeneralSecurityException
import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AesGcmCipherTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val key = newKey()
    private val cipher = AesGcmCipher { key }

    @Test
    fun `descifra lo que cifro`() {
        val plain = "token-secreto-123".encodeToByteArray()

        assertContentEquals(plain, cipher.decrypt(cipher.encrypt(plain)))
    }

    @Test
    fun `el texto plano no aparece en el resultado`() {
        val encrypted = cipher.encrypt("token-secreto-123".encodeToByteArray()).decodeToString()

        assertEquals(false, encrypted.contains("token-secreto"))
    }

    @Test
    fun `el mismo texto cifrado dos veces da resultados distintos por el IV`() {
        val plain = "igual".encodeToByteArray()

        assertNotEquals(cipher.encrypt(plain).toList(), cipher.encrypt(plain).toList())
    }

    @Test
    fun `un dato alterado no se descifra`() {
        val encrypted = cipher.encrypt("dato".encodeToByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        assertFailsWith<GeneralSecurityException> { cipher.decrypt(encrypted) }
    }

    @Test
    fun `una clave distinta no descifra`() {
        val encrypted = cipher.encrypt("dato".encodeToByteArray())
        val other = AesGcmCipher { newKey() }

        assertFailsWith<GeneralSecurityException> { other.decrypt(encrypted) }
    }

    @Test
    fun `un dato demasiado corto se rechaza`() {
        assertFailsWith<IllegalArgumentException> { cipher.decrypt(ByteArray(5)) }
    }
}
