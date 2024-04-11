package server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toJavaInstant
import lib.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    )
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    routing {
        sessions()
        messages()
    }
}

typealias SignedSendSession = SignedMessage<SendSession>
typealias SignedGetSession = SignedMessage<GetSessions>

fun Routing.sessions() {
    val sessions = ConcurrentHashMap<Signatures.PublicKey, Set<SignedSendSession>>()
    post("/session") {
        try {
            val msg = call.receive<SignedSendSession>()
            sessions.compute(msg.getMessage<SendSession>().target) { _, set -> (set ?: setOf()) + msg }
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e)
        }
    }
    get("/session") {
        try {
            val msg = call.receive<SignedGetSession>()

            val time = msg.getMessage<GetSessions>().time
            require(time.toJavaInstant().within(Duration.ofMinutes(1)))

            val found = sessions.remove(msg.publicKey)

            call.respond(found ?: setOf())
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest,e)
        }
    }
}

private typealias SignedSendMessage = SignedMessage<SendMessage>
private typealias SignedGetMessage = SignedMessage<GetMessages>

fun Routing.messages() {
    val messages = ConcurrentHashMap<Signatures.PublicKey, Set<SignedSendMessage>>()
    post("/message") {
        try {
            val m = call.receive<SignedSendMessage>()
            messages.compute(m.getMessage<SendMessage>().target) { _, set -> (set ?: setOf()) + m }
            println("Received {$messages}")
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
    get("/message") {
        try {
            val message = call.receive<SignedGetMessage>()

            val time = message.getMessage<GetMessages>().time
            require(time.toJavaInstant().within(Duration.ofMinutes(1)))

            call.respond(messages.remove(message.publicKey) ?: setOf())
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}

fun Instant.within(duration: Duration): Boolean {
    val time = this
    val now = Instant.now()
    return time.isAfter(now.minus(duration)) && time.isBefore(now.plus(duration))
}
