import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import lib.Base16
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.experimental.xor

@Composable
@Preview
fun App() {
    var user by remember { mutableStateOf(getDefaultUser()) }

    Row(Modifier.fillMaxSize()) {
        //chats
        Column(
            Modifier
                .fillMaxHeight()
                .width(300.dp)
                .verticalScroll(rememberScrollState())
                .background(Color.Magenta)
                .height(IntrinsicSize.Max)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Blue)
            ) {
                Account(user) {
                    user = it
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Yellow)
            ) {
                Button(onClick = { }) {
                    Text("Chats")
                }
            }

        }
        //single chat
        Column(Modifier.fillMaxSize().background(Color.Green)) {
            Button(onClick = { }) {
                Text("Button")
            }
        }
    }
}

@Composable
fun Account(user: User, updateUser: (User) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.clickable {
            expanded = !expanded
        }
            .fillMaxWidth()
    ) {
        UserIcon(user)
        Text(
            text = AnnotatedString(transaction {
                "0x" + Base16.encode(user.publicKey.bytes).substring(0, 8)
            }),
            modifier = Modifier
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            content = {
                transaction {
                    getAccounts()
                }.forEach {
                    DropdownMenuItem(
                        content = {
                            UserIcon(it)
                            Text(transaction { "0x" + Base16.encode(it.publicKey.bytes).substring(0, 8) })
                        },
                        onClick = {
                            expanded = false
                            updateUser(it)
                        }
                    )
                }
                DropdownMenuItem(
                    content = {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Create account")
                    },
                    onClick = {
                        expanded = false
                        val newUser = transaction {
                            User.new {
                                val key = newPrivateKey()
                                privateKey = key
                                publicKey = key.publicKey
                            }
                        }
                        updateUser(newUser)
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
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
            .size(40.dp, 40.dp)
            .padding(8.dp),
        painter = BitmapPainter(
            image = loadImageBitmap(input),
            filterQuality = FilterQuality.None
        ),
        contentDescription = user.toString()
    )
}
