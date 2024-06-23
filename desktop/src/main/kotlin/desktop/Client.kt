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
import lib.Crypto.Signature
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class User(
    val privateKey: Signature.PrivateKey = Signature.generatePrivateKey(),
    override val publicKey: Signature.PublicKey = privateKey.publicKey(),
) : HasPublicKey

interface HasPublicKey {
    val publicKey: Signature.PublicKey
}

val HasPublicKey.nameShort: String
    get() = "0x" + Base16.encode(publicKey.bytes).substring(0, 8)

val HasPublicKey.nameFull: String
    get() = "0x" + Base16.encode(publicKey.bytes)

@Serializable
data class Chat(
    val from: User,
    val to: Contact
)

enum class Direction { RECEIVED, SENT }

data class ChatMessage(val direction: Direction, val text: String);

object Users {
    private val usersMap = ConcurrentHashMap(
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

    fun userChats(user: User) = usersMap[user]!!.chatsStateFlow

    fun chatMessages(chat: Chat) = usersMap[chat.from]?.messagesForChat(chat)
}

@Serializable
@JvmInline
value class Contact(override val publicKey: Signature.PublicKey) : HasPublicKey

const val generateMockMessages = true
val client = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}


val dialog = listOf(
    "Добрый день, Сергей. Можем обсудить сроки по нашему конфиденциальному проекту? Нужно уточнить некоторые моменты.",
    "Здравствуйте, Ирина. Конечно, могу выделить время сейчас. Какие у нас приоритеты?",
    "В первую очередь, хочу напомнить о важности соблюдения конфиденциальности. Этот проект имеет высокий приоритет, и нам нужно уложиться в сроки. Как идёт работа по первой фазе?",
    "Понимаю, держим всё под контролем. Первая фаза почти завершена, осталось несколько технических моментов, которые мы планируем закрыть к середине следующей недели.",
    "Отлично. Помните, что нам нужно завершить весь проект к 15 июля. Сможем ли мы придерживаться этого графика?",
    "Да, мы это понимаем. Если не возникнет непредвиденных задержек, всё будет готово к 15 июля. На всякий случай закладываем пару дней на тестирование и проверку.",
    "Прекрасно. Оперативно сообщайте о любых рисках или задержках. Согласуйте с командой, чтобы все были в курсе сроков и приоритетов.",
    "Обязательно. Команда в курсе. Провожу ежедневные проверки статуса выполнения задач.",
    "Спасибо, Сергей. Поддерживайте нас в курсе. Нам важно, чтобы всё прошло гладко и вовремя.",
    "Без проблем, Ирина. Буду держать вас в курсе всех этапов. Если возникнут вопросы, сразу свяжусь с вами.",
    "Отлично. Надеюсь, что всё пройдет по плану. Спасибо за вашу работу!",
    "Спасибо за доверие. Сделаем всё возможное!",
)

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
                tickerFlow(Duration.ofSeconds(20))
                    .onEach {
                        addChat(Chat(user, Contact(Signature.generatePrivateKey().publicKey())))
                    }
                    .collect()
            }
            // создаем сообщения
            CoroutineScope(Dispatchers.IO).launch {
                tickerFlow(Duration.ofSeconds(1))
                    .flatMapConcat {
                        flow {
                            chatsMap.values.forEach { chat -> emit(chat) }
                        }

                    }
                    .onEach { flow ->
                        val messages = flow.value
                        val newMessage = dialog.getOrNull(messages.size)
                        val direction = Direction.entries[messages.size % 2]
                        println(direction)
                        newMessage?.let {
                            flow.value += ChatMessage(direction, newMessage)
                        }
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

