package lib

import kotlinx.serialization.Serializable
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encrypt {

    @Serializable
    @JvmInline
    value class CypherText(
        @Serializable(with = ByteArrayStringSerializer::class)
        val bytes: ByteArray
    ) {
        init {
            require(bytes.isNotEmpty()) { "message must not be empty" }
        }
    }

    @Serializable
    @JvmInline
    value class Key(
        @Serializable(with = ByteArrayStringSerializer::class)
        val bytes: ByteArray = SecureRandom().generateSeed(32)
    ) {
        init {
            require(bytes.size == 32) { "key size must be 32 bytes" }
        }
    }

    @Serializable
    @JvmInline
    value class Nonce(
        @Serializable(with = ByteArrayStringSerializer::class)
        val bytes: ByteArray = randomBytes(12)
    ) {
        init {
            require(bytes.size == 12) { "bytes must be 12 bytes" }
        }
    }

    /**
     * ChaCha20 encrypt
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(message: PlainText, key: Key, nonce: Nonce): CypherText {
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.bytes, "ChaCha20-Poly1305"),
            IvParameterSpec(nonce.bytes)
        )
        return CypherText(cipher.doFinal(message.bytes))
    }

    /**
     * ChaCha20 decrypt
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(message: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")

        cipher.init(
            Cipher.DECRYPT_MODE, SecretKeySpec(key, "ChaCha20-Poly1305"), IvParameterSpec(nonce)
        )
        return cipher.doFinal(message)
    }

}