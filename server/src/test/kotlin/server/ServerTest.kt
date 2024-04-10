package server

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.*
import lib.KeyExchange.publicKey
import lib.Signatures.publicKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun test() = testApplication {
        application {
            module()
        }

        application {
            routing {
                println(getAllRoutes())
            }
        }

        val privateKey = Signatures.PrivateKey()
        val target = Signatures.PrivateKey()

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val send = SignedMessage.sign(
            SendSession(KeyExchange.PrivateKey().publicKey, target.publicKey),
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
            println("Get plaintext ${String(getSessions.message.bytes)}")
            println("Get Body ${Json.encodeToString(getSessions)}")
            println("Get Msg ${Json.encodeToString(String(getSessions.message.bytes))}")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val actual = body<Set<SignedMessage<SendSession>>>()
            println("Real body ${bodyAsText()}")
            val expected = setOf(send)

            println("Expected ${Json.encodeToString(expected)}")
            println("Expected msg ${String(expected.first().message.bytes)}")

            println("Actual ${Json.encodeToString(actual)}")
            println("Actual msg ${String(actual.first().message.bytes)}")

            assertEquals(expected, actual)
        }
    }
}
