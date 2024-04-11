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
import lib.GetSessions
import lib.SendSession
import lib.Signatures
import lib.Signatures.publicKey
import lib.SignedMessage
import java.time.Duration
import kotlin.random.Random

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
    val messages = service.messages
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

const val generateMockMessages = true

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
        if (generateMockMessages)
            CoroutineScope(Dispatchers.IO).launch {
                tickerFlow(Duration.ofSeconds(5))
                    .onEach {
                        chatsFlow.value += Chat(user, Contact(Signatures.PrivateKey().publicKey))
                    }
                    .collect()
            }
    }
}

class ChatService(val chat: Chat) {
    enum class Direction { RECEIVED, SENT }

    data class ChatMessage(val direction: Direction, val text: String);

    private val messagesFlow: MutableStateFlow<List<ChatMessage>> = MutableStateFlow(listOf())

    val messages = messagesFlow.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tickerFlow(Duration.ofSeconds(1))
                .onEach { }
                .collect()
        }

        if (generateMockMessages) CoroutineScope(Dispatchers.IO).launch {
            tickerFlow(Duration.ofSeconds(1))
                .onEach {
                    val d = if (Random.nextBoolean()) Direction.RECEIVED else Direction.SENT
                    messagesFlow.value += ChatMessage(d, d.toString())
                }
                .collect()
        }
    }
}


fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

