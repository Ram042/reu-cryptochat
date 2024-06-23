package server

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import lib.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @BeforeTest
    fun init() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun testSessions() = testApplication {
        application {
            module()
        }

        application {
            routing {
                println(getAllRoutes())
            }
        }

        val privateKey = Crypto.Signature.generatePrivateKey()
        val target = Crypto.Signature.generatePrivateKey()

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val send = SignedMessage.sign(
            SendSession(Crypto.KeyAgreement.generatePrivateKey().publicKey(), target.publicKey()),
            privateKey
        )

        client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(send)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val getSessions = SignedMessage.sign(
            GetSessions(),
            target
        )
        client.get("/session") {
            contentType(ContentType.Application.Json)
            setBody(getSessions)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val actual = body<Set<SignedMessage<SendSession>>>()
            val expected = setOf(send)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun testMessages() = testApplication {
        application {
            module()
        }

        application {
            routing {
                println(getAllRoutes())
            }
        }

        val msg = "Hello World!"

        val aPrivateKey = Crypto.Signature.generatePrivateKey()
        val aSessionKey = Crypto.KeyAgreement.generatePrivateKey()
        val bPrivateKey = Crypto.Signature.generatePrivateKey()
        val bSessionKey = Crypto.KeyAgreement.generatePrivateKey()

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val send = SignedMessage.sign(
            SendMessage(
                target = bPrivateKey.publicKey(),
                message = SendMessage.EnvelopePayload(message = msg),
                key = Crypto.KeyAgreement.sharedKey(aSessionKey, bSessionKey.publicKey())
            ),
            aPrivateKey
        )

        client.post("/message") {
            contentType(ContentType.Application.Json)
            setBody(send)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        val getMessages = SignedMessage.sign(
            GetMessages(),
            bPrivateKey
        )
        client.get("/message") {
            contentType(ContentType.Application.Json)
            setBody(getMessages)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val actual = body<Set<SignedMessage<SendMessage>>>()
            val expected = setOf(send)
            assertEquals(expected, actual)
        }
    }
}
