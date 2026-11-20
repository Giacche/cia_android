package com.ueno.cia.storage

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifrado autenticado AES-256-GCM. La clave llega por [key]: en producción sale del Android Keystore
 * ([AndroidKeystoreKeyProvider]) y en los tests de JVM es una clave de software, por eso esta clase no depende de Android.
 *
 * Formato del resultado: `IV (12 bytes) || texto cifrado + etiqueta de autenticación`. El IV es nuevo en cada cifrado:
 * reutilizarlo con la misma clave rompería la seguridad de GCM.
 */
class AesGcmCipher(private val key: () -> SecretKey) {
    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain)
    }

    /** Lanza [GeneralSecurityException] si el dato fue alterado o la clave no es la que lo cifró. */
    fun decrypt(data: ByteArray): ByteArray {
        require(data.size > IV_SIZE) { "Dato cifrado demasiado corto" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, data, 0, IV_SIZE))
        return cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
