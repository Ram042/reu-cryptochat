package lib

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA3Digest
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.math.ec.rfc8032.Ed25519
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun pbkdf2(password: String, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return factory.generateSecret(
            PBEKeySpec(
                password.toCharArray(), salt,
                iterations, keyLength
            )
        ).encoded
    }

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

    object Sign {
        const val PRIVATE_KEY_SIZE: Int = 256
        const val PRIVATE_KEY_ARRAY_SIZE: Int = PRIVATE_KEY_SIZE / 8
        const val PUBLIC_KEY_SIZE: Int = 256
        const val PUBLIC_KEY_ARRAY_SIZE: Int = PRIVATE_KEY_SIZE / 8

        fun generatePublicKey(privateKey: ByteArray?): ByteArray {
            val pub = ByteArray(256 / 8)
            Ed25519.generatePublicKey(privateKey, 0, pub, 0)
            return pub
        }

        fun generatePrivateKey(): ByteArray {
            val key = ByteArray(256 / 8)
            SecureRandom().nextBytes(key)
            return key
        }
    }

    object DH {
        fun generatePublicKey(privateKey: ByteArray?): ByteArray {
            return X25519PrivateKeyParameters(privateKey).generatePublicKey().encoded
        }

        fun generatePrivateKey(): ByteArray {
            val key = ByteArray(256 / 8)
            SecureRandom().nextBytes(key)
            return key
        }

        fun generateSharedKey(privateKey: ByteArray?, publicKey: ByteArray?): ByteArray {
            val privateParams = X25519PrivateKeyParameters(privateKey)
            val publicParams = X25519PublicKeyParameters(publicKey)

            val agreement = X25519Agreement()
            agreement.init(privateParams)

            val result = ByteArray(32)
            agreement.calculateAgreement(publicParams, result, 0)
            return result
        }
    }

    object Hash {

        fun SHA3_256(`in`: ByteArray): ByteArray {
            val digest = SHA3Digest(256)
            digest.update(`in`, 0, `in`.size)
            val out = ByteArray(256 / 8)
            digest.doFinal(out, 0)
            return out
        }

        fun SHA256(`in`: ByteArray): ByteArray {
            val sha = SHA256Digest()
            sha.update(`in`, 0, `in`.size)
            val out = ByteArray(32)
            sha.doFinal(out, 0)
            return out
        }
    }
}
