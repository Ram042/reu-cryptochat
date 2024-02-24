
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun App() {
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
                Button(onClick = { }) {
                    Text("Profile")
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
fun Accounts() {

}

