package server.xodus

import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.bindings.StringBinding
import jetbrains.exodus.env.Store
import jetbrains.exodus.env.Transaction

class UserDatabase(private val store: Store) {
    fun addUser(id: ByteArray, profile: String) {
        store.environment.executeInTransaction { txn ->
            store.put(
                txn, ArrayByteIterable(id), StringBinding.stringToEntry(profile)
            )
        }
    }

    fun getUser(id: ByteArray): ByteArray? {
        val raw =
            store.environment.computeInReadonlyTransaction { txn: Transaction? -> store[txn!!, ArrayByteIterable(id)] }
        if (raw == null) {
            return null
        }
        val bytes = raw.bytesUnsafe

        if (bytes.size != raw.length) {
            //TODO check
            throw RuntimeException("Wrong size of data")
        }

        return bytes
    }

}
