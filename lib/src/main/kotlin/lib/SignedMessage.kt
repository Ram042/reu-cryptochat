package lib

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.Signatures.publicKey
import java.util.*

@Serializable
data class SignedMessage<T>(
    val message: PlainText,
    val publicKey: Signatures.PublicKey,
    val signature: Signatures.Signature,
) where T : Message {

    inline fun <reified M> getMessage(): M where M : T = Json.decodeFromString<M>(String(message.bytes))

    init {
        require(Signatures.verify(publicKey, signature, message)) { "bad signature" }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SignedMessage<*>) return false
        // signature не проверяем т.к. подпись валидна
        return message == other.message && publicKey == other.publicKey
    }

    override fun hashCode(): Int {
        // signature не проверяем т.к. подпись валидна
        return Objects.hash(publicKey, message)
    }

    companion object {
        inline fun <reified T : Message> sign(message: T, privateKey: Signatures.PrivateKey): SignedMessage<T> {
            val plainText = PlainText(Json.encodeToString<T>(message).encodeToByteArray())
            return SignedMessage<T>(
                plainText,
                privateKey.publicKey,
                Signatures.sign(privateKey, plainText)
            )
        }
    }
}
