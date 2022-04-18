package server.xodus;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.env.Store;
import server.db.UserDatabase;

public class XodusUserDatabase implements UserDatabase {

    private Store store;

    public XodusUserDatabase(Store store) {
        this.store = store;
    }

    @Override
    public void addUser(byte[] id, byte[] profile) {
        store.getEnvironment().executeInTransaction(txn ->
                store.put(txn, new ArrayByteIterable(id), new ArrayByteIterable(profile)));
    }

    @Override
    public byte[] getUser(byte[] id) {
        var raw = store.getEnvironment().computeInReadonlyTransaction(txn ->
                store.get(txn, new ArrayByteIterable(id)));
        if (raw==null){
            return null;
        }
        var bytes = raw.getBytesUnsafe();

        if (bytes.length!=raw.getLength()){
            //TODO check
            throw new RuntimeException("Wrong size of data");
        }

        return bytes;
    }

    @Override
    public boolean removeUser(byte[] id) {
        return store.getEnvironment().computeInTransaction(txn ->
                store.delete(txn, new ArrayByteIterable(id)));
    }
}
