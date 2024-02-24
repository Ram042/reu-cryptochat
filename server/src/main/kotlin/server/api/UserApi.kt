package server.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.Crypto
import lib.RegisterUserMessage
import lib.SignedMessage
import org.bouncycastle.util.encoders.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import server.xodus.UserDatabase
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@RestController
class UserApi(private val database: UserDatabase) {
    private val logger: Logger = LoggerFactory.getLogger(UserApi::class.java)

    @GetMapping("/user/{id}")
    fun get(@PathVariable id: String): String {
        val user = database.getUser(Base64.decode(id))

        if (user == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        } else {
            return Base64.encode(user)
        }
    }

    @PostMapping
    fun create(@RequestBody msg: SignedMessage<RegisterUserMessage>) {
        try {
            assert(msg.verify())

            val id = Crypto.Hash.SHA256(msg.publicKey)
            database.addUser(id, Json.encodeToString(msg))

            logger.info("Added user {}", Hex.toHexString(id))
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}
