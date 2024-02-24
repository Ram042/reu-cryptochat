package server.api

import kotlinx.datetime.toJavaInstant
import lib.Action
import lib.GetSessionsMessage
import lib.SessionUpdateMessage
import lib.SignedMessage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import server.xodus.SessionDatabase
import java.time.Duration
import java.time.Instant

@RestController
class SessionApi(val sessionDatabase: SessionDatabase) {

    @PostMapping("/session")
    fun addInit(@RequestBody m: SignedMessage<SessionUpdateMessage>) {


        if (m.verify(Action.SESSION_UPDATE)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        sessionDatabase.addSessionInit(m)
    }


    @GetMapping("/session")
    fun getInit(m: SignedMessage<GetSessionsMessage>): List<SignedMessage<SessionUpdateMessage>> {

        if (m.verify(Action.SESSION_GET)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
        //time within 10 minutes (+- 5 minutes)
        val time = m.decodedMessage.time

        if (
            time.toJavaInstant().isBefore(Instant.now().minus(Duration.ofMinutes(1))) ||
            time.toJavaInstant().isAfter(Instant.now().plus(Duration.ofMinutes(1)))
        ) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "bad time")
        }

       return sessionDatabase.getSessionUpdates(m.publicKey)
    }
}


