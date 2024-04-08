package lib

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encrypt {
    /**
     * ChaCha20 encrypt
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(message: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")

        cipher.init(
            Cipher.ENCRYPT_MODE, SecretKeySpec(key, "ChaCha20-Poly1305"),
            IvParameterSpec(nonce)
        )
        return cipher.doFinal(message)
    }

    /**
     * ChaCha20 decrypt
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(message: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")

        cipher.init(
            Cipher.DECRYPT_MODE, SecretKeySpec(key, "ChaCha20-Poly1305"),
            IvParameterSpec(nonce)
        )
        return cipher.doFinal(message)
    }

    fun generateKey(): ByteArray = SecureRandom().generateSeed(32)
}