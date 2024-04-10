package lib

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class MessageTest {

    @Test
    fun testMessage() {
        val getSessions = GetSessions()
        println("Json ${Json.encodeToString(getSessions)}")
        println("PlainText ${String(getSessions.toPlainText().bytes)}")

        val signed = SignedMessage.sign(getSessions, Signatures.PrivateKey())
        println("Signed plaintext ${String(signed.message.bytes)}")
        println("Signed message ${signed.getMessage<GetSessions>()}")
    }


}