package lib

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageTest {

    @Test
    fun testSerialization() {
        println(Json.encodeToString(GetMessageEnvelope()))
    }


    @Test
    fun testMessageEncrypt() {
        val key = Encrypt.generateKey()

        val message = SendMessageEnvelope(
            UUID.randomUUID().toString(),
            Sign.generatePublicKey(Sign.generatePrivateKey()),
            message = SendMessageEnvelope.EnvelopePayload(message = "Hello!"),
            key = key
        )

        println(Json.encodeToString(message))
    }

    @Test
    fun testMessageSigning() {
        val message = GetMessageEnvelope()
        val key = Sign.generatePrivateKey()

        val signedMessage = SignedMessage(message, key)

        assertThat(signedMessage.verify()).isTrue()

        println(Json.encodeToString(signedMessage))
    }


}