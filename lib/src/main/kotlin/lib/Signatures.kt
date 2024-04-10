package lib

import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.math.ec.rfc8032.Ed25519

object Signatures {

    @Serializable
    data class PublicKey(
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
        val bytes: ByteArray = randomBytes(32)
    )

    @Serializable
    @JvmInline
    value class Signature(
        @Serializable(with = ByteArrayStringSerializer::class)
        val bytes: ByteArray
    ) {
        init {
            require(bytes.isNotEmpty()) { "signature must not be empty" }
        }
    }

    public val PrivateKey.publicKey: PublicKey
        get() = PublicKey(generatePublicKey(this.bytes))

    fun generatePublicKey(privateKey: ByteArray?): ByteArray {
        val pub = ByteArray(256 / 8)
        Ed25519.generatePublicKey(privateKey, 0, pub, 0)
        return pub
    }

    public fun sign(privateKey: PrivateKey, plainText: PlainText): Signature {
        return Signature(Ed25519Signer().apply {
            init(true, Ed25519PrivateKeyParameters(privateKey.bytes))
            update(plainText.bytes, 0, plainText.bytes.size)
        }.generateSignature())
    }

    public fun verify(publicKey: PublicKey, signature: Signature, plainText: PlainText): Boolean {
        return Ed25519Signer().apply {
            init(false, Ed25519PublicKeyParameters(publicKey.bytes))
            update(plainText.bytes, 0, plainText.bytes.size)
        }.verifySignature(signature.bytes)
    }
}