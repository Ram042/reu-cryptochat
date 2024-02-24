package lib

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class SignedMessage<T : Message>(
    @Serializable(with = ByteArrayStringSerializer::class)
    val message: ByteArray,
    @Serializable(with = ByteArrayStringSerializer::class)
    val publicKey: ByteArray,
    @Serializable(with = ByteArrayStringSerializer::class)
    val signature: ByteArray,
) {


    constructor(message: T, privateKey: ByteArray) : this(
        Json.encodeToString<Message>(message).encodeToByteArray(),
        Signer.getPublicKeyForPrivate(privateKey),
        Signer.sign(privateKey, Json.encodeToString<Message>(message).encodeToByteArray())
    ) {
        verify()
    }

    fun verify(): Boolean {
        return Signer.verify(publicKey, signature, message)
    }

    fun verify(action: Action): Boolean {
        return this.action == action && verify()
    }

    val action: Action
        get() = Json.decodeFromString<Message>(String(message)).action

    val decodedMessage: T = run {
        val json = Json {
            ignoreUnknownKeys = true
        }
        when (Json.decodeFromString<Message>(String(message)).action) {
            Action.USER_REGISTER -> json.decodeFromString<RegisterUserMessage>(String(message))
            Action.SESSION_UPDATE -> json.decodeFromString<SessionUpdateMessage>(String(message))
            Action.SESSION_GET -> json.decodeFromString<GetSessionsMessage>(String(message))
            Action.ENVELOPE -> json.decodeFromString<SendMessageEnvelope>(String(message))
            Action.ENVELOPE_GET -> json.decodeFromString<GetMessageEnvelope>(String(message))
        } as T
    }
}
