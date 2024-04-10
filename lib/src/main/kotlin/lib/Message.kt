package lib

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.GeneralSecurityException

@Serializable
sealed class Message

fun Message.toPlainText(): PlainText = PlainText(Json.encodeToString<Message>(this).encodeToByteArray())

@Serializable
class SendSession(
    val sessionPublicKey: KeyExchange.PublicKey,
    val target: Signatures.PublicKey
) : Message()

@Serializable
data class GetSessions(
    @Serializable
    val time: Instant = Clock.System.now()
) : Message()

@Serializable
data class SendMessage(
    val target: Signatures.PublicKey,
    val nonce: Encrypt.Nonce,
    val encryptedPayload: Encrypt.CypherText
) : Message() {

    constructor(
        target: Signatures.PublicKey,
        nonce: Encrypt.Nonce = Encrypt.Nonce(),
        message: EnvelopePayload,
        key: Encrypt.Key
    ) : this(
        target,
        nonce,
        Encrypt.encrypt(padMessage(message), key, nonce)
    )

    @Throws(GeneralSecurityException::class)
    fun decrypt(key: ByteArray): EnvelopePayload {
//        return EnvelopePayload(Crypto.Encrypt.decrypt(encryptedPayload, key, nonce))
        TODO()
    }

    @Serializable
    class EnvelopePayload(
        val time: Instant = Clock.System.now(),
        val message: String
    )
}

@Serializable
data class GetMessages(val time: Instant = Clock.System.now()) : Message()


fun padMessage(message: SendMessage.EnvelopePayload): PlainText {
    val messageBytes: ByteArray = message.toString().encodeToByteArray()

    //padding
    val newSize = (messageBytes.size / 64) * 64 + 64
    val messagePaddedBytes = ByteArray(newSize)
    System.arraycopy(messageBytes, 0, messagePaddedBytes, 0, messageBytes.size)
    return PlainText(messagePaddedBytes)
}



