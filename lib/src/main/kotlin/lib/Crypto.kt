package lib

import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA3Digest
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Serializable
@JvmInline
value class PublicKey(val bytes: ByteArray)

@Serializable
@JvmInline
value class PrivateKey(val bytes: ByteArray)

val PrivateKey.publicKey: PublicKey
    get() = PublicKey(Sign.generatePublicKey(this.bytes))

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
