package desktop

import kotlinx.serialization.Serializable
import lib.Message
import lib.PublicKey
import lib.User

@Serializable
@JvmInline
value class Contact(val publicKey: PublicKey)

@Serializable
data class Chat(
    val from: User,
    val to: Contact,
    val messages: List<Message> = listOf()
)
