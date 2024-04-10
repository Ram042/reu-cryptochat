package desktop

import kotlinx.serialization.Serializable
import lib.Message
import lib.Signatures

@Serializable
@JvmInline
value class Contact(val publicKey: Signatures.PublicKey)

@Serializable
data class Chat(
    val from: User,
    val to: Contact,
    val messages: List<Message> = listOf()
)
