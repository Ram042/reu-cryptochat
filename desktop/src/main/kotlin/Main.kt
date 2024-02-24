import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

fun main() = application {
    Database.connect("jdbc:sqlite:chats.db", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        SchemaUtils.create(Users)
    }

    val user = getDefaultUser()

    Window(onCloseRequest = ::exitApplication, title = "KotlinProject") {
        App()
    }
}

fun getDefaultUser(): User = transaction {
    User.find {
        Users.privateKey.isNotNull()
    }.firstOrNull()
} ?: transaction {
    User.new {
        val key = newPrivateKey()
        privateKey = key
        publicKey = key.publicKey
    }
}