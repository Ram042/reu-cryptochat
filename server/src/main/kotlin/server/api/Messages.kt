package server.api

import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.Action
import lib.GetMessageEnvelope
import lib.SendMessageEnvelope
import lib.SignedMessage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import server.xodus.MessageDatabase
import java.time.Duration
import java.time.Instant

@RestController
open class Messages(val messageDatabase: MessageDatabase) {

    @PostMapping("/message")
    fun addUser(@RequestBody message: SignedMessage<SendMessageEnvelope>) {

        if (message.verify(Action.ENVELOPE)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        messageDatabase.addMessage(message.decodedMessage.target, Json.encodeToString(message))
    }

    @GetMapping("/message")
    fun getUser(@RequestBody message: SignedMessage<GetMessageEnvelope>): List<SignedMessage<SendMessageEnvelope>> {
        if (!message.verify(Action.ENVELOPE_GET)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        //time within 10 minutes (+- 5 minutes)
        val time = message.decodedMessage.time
        if (time.toJavaInstant().isBefore(Instant.now().minus(Duration.ofMinutes(1))) ||
            time.toJavaInstant().isAfter(Instant.now().plus(Duration.ofMinutes(1)))
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "bad time")
        }

        return messageDatabase.getMessages(message.publicKey)
    }
}
