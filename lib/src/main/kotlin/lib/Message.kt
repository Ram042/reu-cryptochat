package lib

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.security.GeneralSecurityException
import java.security.SecureRandom

@Serializable
sealed class Message(val action: Action)

@Serializable
class GetMessageEnvelope(
    val time: Instant = Clock.System.now()
) : Message(Action.ENVELOPE_GET)

@Serializable
class GetSessionsMessage(
    val time: Instant = Clock.System.now()
) : Message(Action.SESSION_GET)

@Serializable
class RegisterUserMessage : Message(Action.USER_REGISTER)

private fun generateNonce(): ByteArray {
    val nonce = ByteArray(12)
    SecureRandom().nextBytes(nonce)
    return nonce
}

@Serializable
class SendMessageEnvelope(
    val sessionId: String,
    @Serializable(with = ByteArrayStringSerializer::class)
    val target: ByteArray,
    val alg: String = "ChaCha20-Poly1305",
    @Serializable(with = ByteArrayStringSerializer::class)
    val nonce: ByteArray = generateNonce(),
    @Serializable(with = ByteArrayStringSerializer::class)
    val encryptedPayload: ByteArray
) : Message(Action.ENVELOPE) {

    constructor(
        sessionId: String,
        target: ByteArray,
        alg: String = "ChaCha20-Poly1305",
        nonce: ByteArray = generateNonce(),
        message: EnvelopePayload,
        key: ByteArray
    ) : this(
        sessionId, target, alg, nonce,
        encryptMessage(padMessage(message), key, nonce)
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

fun padMessage(message: SendMessageEnvelope.EnvelopePayload): ByteArray {
    val messageBytes: ByteArray = message.toString().encodeToByteArray()

    //padding
    val newSize = (messageBytes.size / 64) * 64 + 64
    val messagePaddedBytes = ByteArray(newSize)
    System.arraycopy(messageBytes, 0, messagePaddedBytes, 0, messageBytes.size)
    return messagePaddedBytes
}

fun encryptMessage(message: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
    return Crypto.Encrypt.encrypt(message, key, nonce)
}

@Serializable
class SessionUpdateMessage(
    val id: String,
    @Serializable(with = ByteArrayStringSerializer::class)
    val sessionPublicKey: ByteArray,
    @Serializable(with = ByteArrayStringSerializer::class)
    val target: ByteArray
) : Message(Action.SESSION_UPDATE)

