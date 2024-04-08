package desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import lib.Message
import lib.User
import kotlin.concurrent.thread


object Users {
    val userServices = MutableStateFlow(
        run {
            val user = User()
            mapOf(user to UserService(user))
        }
    )

    val users = MutableStateFlow(userServices.value.keys.sortedBy(User::publicKey))

    init {
        thread(name = "emit-users", isDaemon = true) {
            runBlocking(Dispatchers.Default) {
                userServices.collect {
                    users.emit(it.keys.sortedBy(User::publicKey))
                }
            }
        }
    }

    fun newUser(user: User = User()) {
        userServices.value += (user to UserService(user))
    }
}

class UserService(val user: User) {
    val chats: MutableStateFlow<List<ChatService>> = MutableStateFlow(listOf())
}

class ChatService(val chat: Chat) {
    val messages: MutableStateFlow<List<Message>> = MutableStateFlow(listOf())
}




