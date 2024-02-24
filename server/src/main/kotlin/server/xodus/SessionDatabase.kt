package server.xodus

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Store
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.SessionUpdateMessage
import lib.SignedMessage

class SessionDatabase(private val store: Store) {

    fun addSessionInit(message: SignedMessage<SessionUpdateMessage>) = store.environment.executeInTransaction { txn ->
        store.put(
            txn,
            ArrayByteIterable(message.decodedMessage.target),
            StringBinding.stringToEntry(Json.encodeToString<SignedMessage<SessionUpdateMessage>>(message))
        )
    }

    fun getSessionUpdates(target: ByteArray): List<SignedMessage<SessionUpdateMessage>> =
        store.environment.computeInReadonlyTransaction { txn ->
            store.openCursor(txn).use { cursor ->
                val list = ArrayList<SignedMessage<SessionUpdateMessage>>()
                val first = cursor.getSearchKey(ArrayByteIterable(target))
                if (first != null) {

                    list.add(Json.decodeFromString(StringBinding.entryToString(first)))

                    while (cursor.nextDup) {
                        val arr = ArrayByteIterable(cursor.value)
                        list.add(Json.decodeFromString(StringBinding.entryToString(arr)))
                    }
                }
                list
            }
        }
}
