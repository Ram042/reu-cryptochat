package desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.experimental.xor
import lib.Base16


fun main() = application {
    val users by Users.users.collectAsState()

    Window(
        onCloseRequest = {
            println(Json.encodeToString(users))
            exitApplication()
        },
        title = "CryptoChat"
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
    onCreateUser: (User) -> Unit
) {
    require(users.isNotEmpty()) { "no users exist" }
    var activeUser by rememberSaveable { mutableStateOf(users.first()) }

    ChatScreen(
        users = users,
        user = activeUser,
        onUserChange = { selectedUser ->
            activeUser = selectedUser
        },
        onCreateUser = { newUser ->
            onCreateUser(newUser)
            activeUser = newUser
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
    onCreateUser: (User) -> Unit
) {
    var chats by remember { mutableStateOf(listOf<Chat>()) }

    Row(Modifier.fillMaxSize()) {
        //chats
        Column(
            Modifier
                .fillMaxHeight()
                .width(300.dp)
                .verticalScroll(rememberScrollState())
                .background(Color.Magenta.copy(alpha = 0.5f))
                .height(IntrinsicSize.Max)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Blue.copy(alpha = 0.5f))
            ) {
                ActiveAccount(
                    users = users,
                    activeUser = user,
                    updateUser = {
                        onUserChange(it)
                    },
                    onCreateUser = {
                        onCreateUser(it)
                    }
                )
            }
            ChatList(chats)

        }
        //single chat
        Column(Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.5f))) {
            Button(onClick = { }) {
                Text("Button")
            }
        }
    }
}

@Composable
fun ChatList(
    chats: List<Chat>
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Yellow.copy(alpha = 0.5f))
    ) {
        Button(onClick = { }) {
            Text("Chats")
        }
    }
}


@Composable
fun ActiveAccount(
    users: List<User>,
    activeUser: User,
    updateUser: (User) -> Unit,
    onCreateUser: (User) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable {
                expanded = !expanded
            }
            .fillMaxWidth()
    ) {
        UserIcon(activeUser)
        Text(
            text = AnnotatedString("0x" + Base16.encode(activeUser.publicKey.bytes).substring(0, 8)),
            modifier = Modifier
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

@Composable
fun AccountSelect(
    expanded: Boolean,
    users: List<User>,
    activeUser: User,
    updateUser: (User) -> Unit,
    onCreateUser: (User) -> Unit,
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
                    onCreateUser(User())
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
