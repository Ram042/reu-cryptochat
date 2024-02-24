package server.xodus

import jetbrains.exodus.env.Environments.newInstance
import jetbrains.exodus.env.StoreConfig
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class Database {
    private val environment = newInstance("cryptochat")

    @get:Bean
    val messageDatabase: MessageDatabase = MessageDatabase(environment.computeInTransaction { txn ->
        environment.openStore(
            "messages",
            StoreConfig.WITH_DUPLICATES,
            txn
        )
    })

    @get:Bean
    val userDatabase: UserDatabase = UserDatabase(environment.computeInTransaction { txn ->
        environment.openStore(
            "users",
            StoreConfig.WITHOUT_DUPLICATES,
            txn
        )
    })

    @get:Bean
    val sessionDatabase: SessionDatabase = SessionDatabase(environment.computeInTransaction { txn ->
        environment.openStore(
            "session",
            StoreConfig.WITH_DUPLICATES,
            txn
        )
    })

}
