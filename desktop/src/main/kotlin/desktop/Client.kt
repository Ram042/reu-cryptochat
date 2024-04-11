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
import lib.*
import lib.Signatures.publicKey
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Serializable
data class User(
    val privateKey: Signatures.PrivateKey = Signatures.PrivateKey(),
    val publicKey: Signatures.PublicKey = privateKey.publicKey,
)

@Serializable
data class Chat(
    val from: User,
    val to: Contact
)

enum class Direction { RECEIVED, SENT }

data class ChatMessage(val direction: Direction, val text: String);

object Users {
    val usersMap = ConcurrentHashMap(
        User().let { user ->
            mapOf(user to UserService(user))
        }
    )

    private val mutableUsersFlow = MutableStateFlow(usersMap.keys().toList())
    val users = mutableUsersFlow

    fun newUser(): User = User().also { user ->
        usersMap += user to UserService(user)
        mutableUsersFlow.value += user
    }

    fun userChats(user: User) = usersMap[user]?.chatsStateFlow

    fun chatMessages(chat: Chat) = usersMap[chat.from]?.messagesForChat(chat)
}

@Serializable
@JvmInline
value class Contact(val publicKey: Signatures.PublicKey)

const val generateMockMessages = true
val client = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
class UserService(val user: User) {
    private val unmappedMessages = MutableSharedFlow<SignedMessage<SendMessage>>()

    private val chatsMap = ConcurrentHashMap<Contact, MutableStateFlow<List<ChatMessage>>>()

    private val mutableChatsStateFlow: MutableStateFlow<List<Chat>> = MutableStateFlow(listOf())
    val chatsStateFlow: StateFlow<List<Chat>> = mutableChatsStateFlow.asStateFlow()

    fun messagesForChat(chat: Chat) = chatsMap[chat.to]?.asStateFlow()

    fun addChat(chat: Chat) {
        require(!chatsMap.containsKey(chat.to)) { "chat already exists" }
        chatsMap[chat.to] = MutableStateFlow(listOf())
        mutableChatsStateFlow.value += chat
    }

    init {
        // получаем чаты
        CoroutineScope(Dispatchers.IO).launch {
            tickerFlow(Duration.ofSeconds(1))
                .map {
                    client.get("http://localhost:8080/session") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            SignedMessage.sign<GetSessions>(
                                GetSessions(),
                                user.privateKey
                            )
                        )
                    }.body<Set<SignedMessage<SendSession>>>()
                }
                .flatMapConcat { messages ->
                    flow {
                        messages.forEach { message -> emit(message) }
                    }
                }
                .onEach {
                    addChat(Chat(user, Contact(it.publicKey)))
                }
                .collect()
        }
        // получаем сообщения
        CoroutineScope(Dispatchers.IO).launch {
            tickerFlow(Duration.ofSeconds(5))
                .map {
                    client.get("http://localhost:8080/message") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            SignedMessage.sign<GetMessages>(
                                GetMessages(),
                                user.privateKey
                            )
                        )
                    }
                }
                .map { it.body<Set<SignedMessage<SendMessage>>>() }
                .flatMapConcat { messages ->
                    flow {
                        messages.forEach { message -> emit(message) }
                    }
                }
                .onEach { m ->
                    val chat = chatsMap[Contact(m.publicKey)]
                    if (chat == null) {
                        unmappedMessages.emit(m)
                    } else {
                        chat.value += ChatMessage(
                            Direction.RECEIVED,
                            m.getMessage<SendMessage>().encryptedPayload.toString()
                        )
                    }
                }
                .collect()
        }
        if (generateMockMessages) {
            // создаем чаты
            CoroutineScope(Dispatchers.IO).launch {
                tickerFlow(Duration.ofMillis(1000))
                    .onEach {
                        addChat(Chat(user, Contact(Signatures.PrivateKey().publicKey)))
                    }
                    .collect()
            }
            // создаем сообщения
            CoroutineScope(Dispatchers.IO).launch {
                tickerFlow(Duration.ofMillis(500))
                    .map {
                        val v = chatsMap.values
                        v
                    }
                    .flatMapConcat { v ->
                        flow {
                            v.forEach { emit(it) }
                        }
                    }
                    .onEach {
                        val d = if (Random.nextBoolean()) Direction.RECEIVED else Direction.SENT
                        it.value += ChatMessage(d, d.toString())
                    }
                    .collect()
            }
        }
    }
}

suspend fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

