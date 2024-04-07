import org.jetbrains.exposed.crypt.Encryptor
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.util.*
import kotlin.test.Test


class DbTest {

    @Test
    fun testEncryption() {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        val db = Database.connect(
            url = "jdbc:sqlite:test.db",
            driver = "org.sqlite.JDBC",
        )


        val tableEncrypted = object : Table("test") {
            val column = encryptedText(
                "test",
                Encryptor(
                    { Base64.getEncoder().encodeToString(it.toByteArray()) },
                    { String(Base64.getDecoder().decode(it)) },
                    { 0 }
                )
            )
        }

        val tableRaw = object : Table("test") {
            val column = text("test")
        }

        transaction(db) {
            SchemaUtils.drop(tableEncrypted)
        }

        transaction(db) {
            SchemaUtils.create(tableEncrypted)
        }

        transaction(db) {
            tableEncrypted.insert {
                it[column] = "123"
            }
        }

        val encrypted = transaction(db) {
            tableRaw.selectAll().first()[tableRaw.column]
        }
        require(encrypted == "MTIz") { "Want MTIz but was $encrypted" }

        val decrypted = transaction(db) {
            tableEncrypted.selectAll().first()[tableEncrypted.column]
        }

        require(decrypted == "123") { "Want 123 but was $decrypted" }
    }

}