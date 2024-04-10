package lib

import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters

object KeyExchange {

    @Serializable
    @JvmInline
    value class PublicKey(
        @Serializable(with = ByteArrayStringSerializer::class)
        val bytes: ByteArray
    )

    @Serializable
    @JvmInline
    value class PrivateKey(
        @Serializable(with = ByteArrayStringSerializer::class)
        val bytes: ByteArray = randomBytes(256 / 8)
    )

    val PrivateKey.publicKey: PublicKey
        get() = PublicKey(X25519PrivateKeyParameters(bytes).generatePublicKey().encoded)

    @Serializable
    @JvmInline
    value class SharedKey(val bytes: ByteArray)

    val SharedKey.encryptionKey: Encrypt.Key
        get() = Encrypt.Key(bytes)

    fun generateSharedKey(privateKey: PrivateKey, publicKey: PublicKey): SharedKey {
        val privateParams = X25519PrivateKeyParameters(privateKey.bytes)
        val publicParams = X25519PublicKeyParameters(publicKey.bytes)

        val agreement = X25519Agreement()
        agreement.init(privateParams)

        val result = ByteArray(32)
        agreement.calculateAgreement(publicParams, result, 0)
        return SharedKey(result)
    }
}