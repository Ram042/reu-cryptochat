import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import lib.Base16
import org.jetbrains.exposed.sql.transactions.transaction

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
    Column {
        ClickableText(
            text = AnnotatedString(transaction {
                "0x" + Base16.encode(user.publicKey.bytes).substring(0, 8)
            }),
            modifier = Modifier,
            onClick = {
                expanded = !expanded
            }
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
                        content = { Text(transaction { "0x" + Base16.encode(it.publicKey.bytes).substring(0, 8) }) },
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


