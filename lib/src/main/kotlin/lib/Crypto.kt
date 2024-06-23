package lib

import kotlinx.serialization.Serializable
import lib.Crypto.Cipher.CipherText
import lib.Crypto.Cipher.IV
import lib.Crypto.Cipher.Key
import lib.Crypto.Signature.PublicKey
import lib.Crypto.Signature.Signature
import org.bouncycastle.jcajce.provider.asymmetric.ecgost12.BCECGOST3410_2012PrivateKey
import org.bouncycastle.jcajce.spec.GOST3410ParameterSpec
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec
import org.bouncycastle.jce.ECGOST3410NamedCurveTable
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger.ONE
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object Crypto {
    object Signature {
        @Serializable
        data class PublicKey(
            @Serializable(with = ByteArrayStringSerializer::class)
            override val bytes: ByteArray
        ) : HasBytes<PublicKey>, Comparable<PublicKey> {
            override fun compareTo(other: PublicKey): Int {
                val a = this.bytes
                val b = other.bytes
                for (i in 0 until a.size) {
                    val c = a[i].compareTo(b[i])
                    if (c != 0) return c
                }
                return 0
            }

            override fun equals(other: Any?): Boolean = hasBytesEquals(other)

            override fun hashCode(): Int = bytes.contentHashCode()
        }

        @Serializable
        @JvmInline
        value class PrivateKey(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray
        ) {
            init {
                require(bytes.isNotEmpty()) { "Empty" }
            }
        }

        @Serializable
        @JvmInline
        value class Signature(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray
        ) {
            init {
                require(bytes.isNotEmpty()) { "Empty" }
            }
        }

    }

    object KeyAgreement {
        @Serializable
        @JvmInline
        value class PublicKey(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray
        ) {
            init {
                require(bytes.isNotEmpty()) { "Empty" }
            }
        }

        @Serializable
        @JvmInline
        value class PrivateKey(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray
        ) {
            init {
                require(bytes.isNotEmpty()) { "Empty" }
            }
        }

        @Serializable
        @JvmInline
        value class SharedKey(
            val bytes: ByteArray
        ) {
            init {
                require(bytes.isNotEmpty()) { "Empty" }
            }
        }
    }

    object Cipher {
        @Serializable
        @JvmInline
        value class CipherText(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray
        ) {
            init {
                require(bytes.isNotEmpty()) { "Empty" }
            }
        }

        @Serializable
        @JvmInline
        value class Key(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray = randomBytes(32)
        ) {
            init {
                require(bytes.size == 32) { "Size ust be 32" }
            }
        }

        @Serializable
        @JvmInline
        value class IV(
            @Serializable(with = ByteArrayStringSerializer::class)
            val bytes: ByteArray = randomBytes(8)
        ) {
            init {
                require(bytes.size == 8) { "Size ust be 8" }
            }
        }
    }
}

// -----------
// Signatures
// -----------

fun Crypto.Signature.generatePrivateKey(): Crypto.Signature.PrivateKey =
    KeyPairGenerator.getInstance("ECGOST3410-2012").run {
        initialize(GOST3410ParameterSpec("Tc26-Gost-3410-12-256-paramSetA"))
        val key = (generateKeyPair().private as BCECGOST3410_2012PrivateKey).encoded
        Crypto.Signature.PrivateKey(key)
    }

private fun Crypto.Signature.PrivateKey.toCryptoPrivateKey(): java.security.PrivateKey =
    KeyFactory.getInstance("ECGOST3410-2012").generatePrivate(PKCS8EncodedKeySpec(bytes))

private fun Crypto.Signature.PublicKey.toCryptoPublicKey(): java.security.PublicKey =
    KeyFactory.getInstance("ECGOST3410-2012").generatePublic(X509EncodedKeySpec(bytes))

fun Crypto.Signature.PrivateKey.publicKey(): Crypto.Signature.PublicKey =
    KeyFactory.getInstance("ECGOST3410-2012").run {
        val ecSpec = ECGOST3410NamedCurveTable.getParameterSpec("Tc26-Gost-3410-12-256-paramSetA")

        val Q = ecSpec.g.multiply((toCryptoPrivateKey() as BCECGOST3410_2012PrivateKey).d)
        val pubSpec = ECPublicKeySpec(Q, ecSpec)
        val public = generatePublic(pubSpec)

        Crypto.Signature.PublicKey(public.encoded)
    }


fun Crypto.Signature.sign(privateKey: Crypto.Signature.PrivateKey, plainText: PlainText): Signature =
    java.security.Signature.getInstance("ECGOST3410-2012-256").run {
        initSign(privateKey.toCryptoPrivateKey())

        update(plainText.bytes)

        Signature(sign())
    }

fun Crypto.Signature.verify(publicKey: PublicKey, signature: Signature, plainText: PlainText): Boolean =
    java.security.Signature.getInstance("ECGOST3410-2012-256").run {
        initVerify(publicKey.toCryptoPublicKey())

        update(plainText.bytes)

        verify(signature.bytes)
    }

// -----------
// Agreement
// -----------

fun Crypto.KeyAgreement.generatePrivateKey() = Crypto.KeyAgreement.PrivateKey(
    Crypto.Signature.generatePrivateKey().bytes
)

private fun Crypto.KeyAgreement.PrivateKey.toCryptoPrivateKey() = Crypto.Signature.PrivateKey(
    bytes
).toCryptoPrivateKey()

fun Crypto.KeyAgreement.PrivateKey.publicKey() = Crypto.KeyAgreement.PublicKey(
    Crypto.Signature.PrivateKey(bytes).publicKey().bytes
)

private fun Crypto.KeyAgreement.PublicKey.toCryptoPublicKey() = Crypto.Signature.PublicKey(
    bytes
).toCryptoPublicKey()

fun Crypto.KeyAgreement.sharedKey(
    privateKey: Crypto.KeyAgreement.PrivateKey,
    publicKey: Crypto.KeyAgreement.PublicKey
) = KeyAgreement.getInstance("ECGOST3410-2012-256").run {
    init(privateKey.toCryptoPrivateKey(), UserKeyingMaterialSpec(ONE.toByteArray()))
    doPhase(publicKey.toCryptoPublicKey(), true)
    Key(generateSecret())
}

// -----------
// Cipher
// -----------

fun Crypto.Cipher.generateKey(): Crypto.Cipher.Key = Crypto.Cipher.Key(
    KeyGenerator.getInstance("GOST3412-2015").generateKey().encoded
)

fun Crypto.Cipher.encrypt(message: PlainText, key: Key, iv: IV): CipherText =
    Cipher.getInstance("GOST3412-2015/CTR/NOPADDING").run {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.bytes, "GOST3412-2015"), IvParameterSpec(iv.bytes))
        
        CipherText(doFinal(message.bytes))
    }


fun Crypto.Cipher.decrypt(cipherText: CipherText, key: Key, iv: IV): PlainText =
    Cipher.getInstance("GOST3412-2015/CTR/NOPADDING").run {
        init(Cipher.DECRYPT_MODE, SecretKeySpec(key.bytes, "GOST3412-2015"), IvParameterSpec(iv.bytes))

        update(cipherText.bytes)

        PlainText(doFinal())
    }

fun randomBytes(count: Int): ByteArray = ByteArray(count).apply { SecureRandom().nextBytes(this) }

interface HasBytes<T> {
    val bytes: ByteArray
}

inline fun <reified T : HasBytes<T>> HasBytes<T>.hasBytesEquals(other: Any?): Boolean {
    if (other !is T) return false
    return bytes.contentEquals(other.bytes)
}

inline fun <reified T : HasBytes<T>> HasBytes<T>.hasBytesHashcode(): Int = bytes.contentHashCode()

@Serializable
data class PlainText(
    @Serializable(with = ByteArrayStringSerializer::class)
    override val bytes: ByteArray
) : HasBytes<PlainText> {
    init {
        require(bytes.isNotEmpty()) { "message must not be empty" }
    }

    override fun equals(other: Any?): Boolean = hasBytesEquals(other)

    override fun hashCode(): Int = bytes.contentHashCode()
}

fun Crypto.KeyAgreement.SharedKey.toKey(): Crypto.Cipher.Key = Crypto.Cipher.Key(bytes)
