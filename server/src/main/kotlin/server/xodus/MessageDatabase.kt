package server.xodus

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Store
import jetbrains.exodus.env.Transaction
import kotlinx.serialization.json.Json
import lib.SendMessageEnvelope
import lib.SignedMessage
import java.util.stream.Collectors

class MessageDatabase(private val store: Store) {
    fun addMessage(target: ByteArray, message: String) {
        store.environment.executeInTransaction { txn ->
            store.put(
                txn,
                ArrayByteIterable(target), StringBinding.stringToEntry(message)
            )
        }
    }

    fun getMessages(target: ByteArray): List<SignedMessage<SendMessageEnvelope>> {
        return store.environment.computeInReadonlyTransaction { txn: Transaction? ->
            val list: ArrayList<ArrayByteIterable> = ArrayList<ArrayByteIterable>()
            val cursor = store.openCursor(txn!!)
            val m = cursor.getSearchKey(ArrayByteIterable(target))
            if (m != null) {
                list.add(ArrayByteIterable(m))

                //                cursor.deleteCurrent();
                while (cursor.nextDup) {
                    list.add(ArrayByteIterable(cursor.value))
                    //                    cursor.deleteCurrent();
                }
            }
            list.stream()
                .map {
                    Json.decodeFromString<SignedMessage<SendMessageEnvelope>>(
                        StringBinding.entryToString(it)
                    )
                }
                .collect(Collectors.toList())
        }
    }
}
