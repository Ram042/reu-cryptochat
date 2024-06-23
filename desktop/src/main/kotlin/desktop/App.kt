package desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.Base16
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jetbrains.skiko.ClipboardManager
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.Security
import javax.imageio.ImageIO
import kotlin.experimental.xor


fun main() = application {
    Security.setProperty("crypto.policy", "unlimited")
    Security.addProvider(BouncyCastleProvider())

    val users by Users.users.collectAsState()

    Window(
        onCloseRequest = {
            println(Json.encodeToString(users))
            exitApplication()
        },
        title = "Защищенный корпоративный мессенджер"
    ) {
        App(
            users = users,
            onCreateUser = {
                Users.newUser()
            }
        )
    }
}

@Composable
fun App(
    users: List<User>,
    onCreateUser: () -> User
) {
    require(users.isNotEmpty()) { "no users exist" }
    var activeUser by rememberSaveable { mutableStateOf(users.first()) }

    ChatScreen(
        users = users,
        user = activeUser,
        onUserChange = { selectedUser ->
            activeUser = selectedUser
        },
        onCreateUser = {
            activeUser = onCreateUser()
        }
    )
}

/**
 * Экран чатов: текущий пользователь, список чатов, сообщения в чате
 */
@Composable
fun ChatScreen(
    users: List<User>,
    user: User,
    onUserChange: (User) -> Unit,
    onCreateUser: () -> Unit
) {
    val chats by Users.userChats(user).collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var chat: Chat? by rememberSaveable { mutableStateOf(null) }
    val messages = chat?.let { Users.chatMessages(it)?.collectAsState() }
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            floatingActionButtonPosition = FabPosition.Start,
            modifier = Modifier
                .fillMaxWidth(0.3f),
            topBar = {
                ActiveAccount(
                    users = users,
                    activeUser = user,
                    updateUser = {
                        onUserChange(it)
                    },
                    onCreateUser = {
                        onCreateUser()
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                ) {
                    Icon(Icons.Rounded.Add, "New chat")
                }
            }
        ) { pad ->
            ChatList(pad, chats) { chat = it }
        }
        Scaffold(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color.Blue.copy(alpha = 0.8f)),
            topBar = {
                chat?.let {
                    Text(
                        text = "Сообщения с пользователем ${it.to.nameShort}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            bottomBar = {
                chat?.let {
                    Row(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = {
                                message = it
                            },
                            label = { Text("Сообщение") },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1.0f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(
                            modifier = Modifier
                                .align(Alignment.CenterVertically),
                            content = { Icon(Icons.AutoMirrored.Rounded.Send, "New chat") },
                            onClick = {}
                        )
                    }
                }

            }
        ) { pad ->
            MessageList(pad, messages?.value ?: listOf())
        }
    }

    NewChat(showDialog, { showDialog = false }) {

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatList(
    pad: PaddingValues,
    chats: List<Chat>,
    onChatSelect: (Chat) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(pad)
    ) {
        Text(text = "Список чатов", fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(chats) { chat ->
                Box(
                    modifier = Modifier
                        .onClick {
                            onChatSelect(chat)
                        }
                ) {
                    Text(text = chat.to.nameShort)
                }
            }
        }
    }

}

@Composable
fun MessageList(
    pad: PaddingValues,
    messages: List<ChatMessage>
) {
    val state = rememberLazyListState(Int.MAX_VALUE)
    LazyColumn(
        modifier = Modifier
            .padding(pad)
            .fillMaxSize(),
        verticalArrangement = Arrangement
            .spacedBy(8.dp),
        state = state
    ) {
        items(messages) { message ->
            val align = when (message.direction) {
                Direction.RECEIVED -> Alignment.TopStart
                Direction.SENT -> Alignment.TopEnd
            }
            val color = when (message.direction) {
                Direction.RECEIVED -> Color.Blue.copy(alpha = 0.15f)
                Direction.SENT -> Color.Blue.copy(alpha = 0.3f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .padding(5.dp)
                        .fillMaxWidth(0.7f)
                        .align(align)
                ) {
                    Text(
                        text = message.text
                    )
                }
            }
        }
    }
}

@Composable
fun NewChat(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onNewChat: (Contact) -> Unit
) {
    var address by remember { mutableStateOf("") }
    DialogWindow(
        visible = showDialog,
        onCloseRequest = onDismiss,
        title = "Новый чат",
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center)
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                    },
                    label = { Text("Адресат") }
                )
                Button(
                    content = {
                        Icon(Icons.AutoMirrored.Rounded.Send, "Start chat")
                    },
                    onClick = {
                        onNewChat(TODO())
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActiveAccount(
    users: List<User>,
    activeUser: User,
    updateUser: (User) -> Unit,
    onCreateUser: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = "Текущий пользователь", fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .clickable {
                    expanded = !expanded
                }
                .fillMaxWidth()
        ) {
            UserIcon(activeUser)
            Text(
                text = activeUser.nameShort,
                modifier = Modifier
            )
            Icon(
                modifier = Modifier.onClick { ClipboardManager().setText(activeUser.nameFull) },
                imageVector = Icons.Default.Done,
                contentDescription = "Copy"
            )
            AccountSelect(
                expanded = expanded,
                users = users,
                activeUser = activeUser,
                updateUser = updateUser,
                onCreateUser = onCreateUser,
                onClose = {
                    expanded = false
                }
            )
        }
    }

}

@Composable
fun AccountSelect(
    expanded: Boolean,
    users: List<User>,
    activeUser: User,
    updateUser: (User) -> Unit,
    onCreateUser: () -> Unit,
    onClose: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            onClose()
        },
        content = {
            users.forEach { user ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier
                                .background(if (user == activeUser) Color.Gray.copy(alpha = 0.1f) else Color.Transparent)
                        ) {
                            UserIcon(user)
                            Text("0x" + Base16.encode(user.publicKey.bytes).substring(0, 8))
                        }
                    },
                    onClick = {
                        onClose()
                        updateUser(user)
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create account")
                },
                onClick = {
                    onClose()
                    onCreateUser()
                }
            )
        }
    )
}

@Composable
fun UserIcon(user: User) {
    val key = user.publicKey.bytes
    val imgSize = 8
    val img = BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB)

    val half = ByteArray(imgSize * imgSize / 2)
    for ((index, byte) in key.withIndex()) {
        half[index % half.size] = half[index % half.size] xor byte
    }

    for (x in 0..<imgSize / 2) {
        for (y in 0..<imgSize) {
            val byte = half[(x + 1) * (y + 1) % half.size]


            val color = java.awt.Color.HSBtoRGB(byte.toUByte().toFloat() / 360f, 0.8f, 0.9f)
            img.setRGB(x, y, color)
            img.setRGB(imgSize - 1 - x, y, color)
        }
    }

    val output = ByteArrayOutputStream()
    ImageIO.write(img, "png", output)
    val input = ByteArrayInputStream(output.toByteArray())

    Image(
        modifier = Modifier
            .size((32 + 8 * 2).dp, (32 + 8 * 2).dp)
            .padding(8.dp),
        painter = BitmapPainter(
            image = loadImageBitmap(input),
            filterQuality = FilterQuality.None
        ),
        contentDescription = user.toString()
    )
}
