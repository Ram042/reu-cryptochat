package server.xodus;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.env.Store;

public class UserDatabase {

    private Store store;

    public UserDatabase(Store store) {
        this.store = store;
    }

    public void addUser(byte[] id, byte[] profile) {
        store.getEnvironment().executeInTransaction(txn ->
                store.put(txn, new ArrayByteIterable(id), new ArrayByteIterable(profile)));
    }

    public byte[] getUser(byte[] id) {
        var raw = store.getEnvironment().computeInReadonlyTransaction(txn ->
                store.get(txn, new ArrayByteIterable(id)));
        if (raw == null) {
            return null;
        }
        var bytes = raw.getBytesUnsafe();

        if (bytes.length != raw.getLength()) {
            //TODO check
            throw new RuntimeException("Wrong size of data");
        }

        return bytes;
    }

    public boolean removeUser(byte[] id) {
        return store.getEnvironment().computeInTransaction(txn ->
                store.delete(txn, new ArrayByteIterable(id)));
    }
}
