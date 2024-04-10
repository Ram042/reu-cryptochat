package desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import lib.Message
import lib.Signatures
import lib.Signatures.publicKey


@Serializable
data class User(
    val privateKey: Signatures.PrivateKey = Signatures.PrivateKey(),
    val publicKey: Signatures.PublicKey = privateKey.publicKey,
)

object Users {
    val userServices = MutableStateFlow(
        run {
            val user = User()
            mapOf(user to UserService(user))
        }
    )

    val users = MutableStateFlow(userServices.value.keys.sortedBy(User::publicKey))

    init {
        CoroutineScope(Dispatchers.Default).launch {
            userServices.collect {
                users.emit(it.keys.sortedBy(User::publicKey))
            }
        }
    }

    fun newUser(user: User = User()) {
        userServices.value += (user to UserService(user))
    }
}

class UserService(val user: User) {
    val chats: MutableStateFlow<List<ChatService>> = MutableStateFlow(listOf())

    init {
        CoroutineScope(Dispatchers.IO).launch {

        }
    }
}

class ChatService(val chat: Chat) {
    val messages: MutableStateFlow<List<Message>> = MutableStateFlow(listOf())

    suspend fun fetchMessages(chat: ChatService) {
        
    }
}




