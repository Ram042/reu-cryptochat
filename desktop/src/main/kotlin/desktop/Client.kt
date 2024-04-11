package desktop

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import lib.*
import lib.Signatures.publicKey
import java.time.Duration

@Serializable
data class User(
    val privateKey: Signatures.PrivateKey = Signatures.PrivateKey(),
    val publicKey: Signatures.PublicKey = privateKey.publicKey,
) {
    @Transient
    private val service = UserService(this)

    @Transient
    val chats = service.chats
}

@Serializable
data class Chat(
    val from: User,
    val to: Contact
) {
    @Transient
    private val service = ChatService(this)

    @Transient
    val chats = service.messages
}

@Serializable
@JvmInline
value class Contact(val publicKey: Signatures.PublicKey)

object Users {

    private val usersFlow = MutableStateFlow(listOf(User()))

    val users = usersFlow.asStateFlow()

    fun newUser(user: User = User()) {
        usersFlow.value += user
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UserService(val user: User) {
    private val chatsFlow: MutableStateFlow<List<Chat>> = MutableStateFlow(listOf())

    val chats = chatsFlow.asStateFlow()

    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tickerFlow(Duration.ofSeconds(1))
                .map {
                    val getSessions = SignedMessage.sign(
                        GetSessions(),
                        user.privateKey
                    )
                    client.get("http://localhost:8080/session") {
                        contentType(ContentType.Application.Json)
                        setBody(getSessions)
                    }.body<Set<SignedMessage<SendSession>>>()
                }
                .flatMapConcat { messages ->
                    flow {
                        messages.forEach { message -> emit(message) }
                    }
                }
                .onEach {
                    chatsFlow.value += Chat(user, Contact(it.publicKey))
                }
                .collect()
        }
    }
}

class ChatService(val chat: Chat) {
    private val messagesFlow: MutableStateFlow<List<Message>> = MutableStateFlow(listOf())

    val messages = messagesFlow.asStateFlow()

    suspend fun fetchMessages(chat: ChatService) {
        tickerFlow(Duration.ofSeconds(1))
            .onEach {
                println("timer")
            }
            .collect()
    }
}


fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

