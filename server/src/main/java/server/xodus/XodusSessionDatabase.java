package server.xodus;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.LongBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import lib.Message;
import lib.SignedMessage;
import lib.message.SessionUpdateMessage;
import server.db.SessionDatabase;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class XodusSessionDatabase implements SessionDatabase {

    private final Store store;

    public XodusSessionDatabase(Store store) {
        this.store = store;
    }

    private static byte[] generateKey(byte[] target, byte type) {
        byte[] result = new byte[32 + 1];

        System.arraycopy(target, 0, result, 0, 32);
        result[32] = type;

        return result;
    }

    private void addMessage(Message message) {
        byte[] key = ((SessionUpdateMessage) message).getTarget();

        byte[] value = ByteBuffer.allocate(Long.BYTES + message.serialize().encode().length)
                .putLong(Instant.now().getEpochSecond())
                .put(message.serialize().encode()).array();

        store.getEnvironment().executeInTransaction(txn ->
                store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value)));
    }

    @Override
    public void addSessionInit(SignedMessage<SessionUpdateMessage> message) {
        addMessage(message.getMessage());
    }

    @Override
    public void addSessionResponse(SignedMessage<SessionUpdateMessage> message) {
        addMessage(message.getMessage());
    }

    public SignedMessage<SessionUpdateMessage>[] getSessionUpdates(byte[] target) {
        var l = store.getEnvironment().computeInReadonlyTransaction(txn -> {
            try (Cursor cursor = store.openCursor(txn)) {
                final ByteIterable v = cursor.getSearchKey(new ArrayByteIterable(target));
                if (v != null) {
                    ArrayList<SignedMessage<SessionUpdateMessage>> list = new ArrayList<>();
                    while (cursor.getNextDup()) {
                        var arr = new ArrayByteIterable(cursor.getValue());
                        list.add(new SignedMessage<>(arr.getBytesUnsafe()));
                    }
                    return list;
                } else {
                    return null;
                }
            }
        });
        return l.toArray(new SignedMessage[0]);
    }

    @Override
    public void prune() {
        store.getEnvironment().executeInTransaction(txn -> {
            try (Cursor cursor = store.openCursor(txn)) {
                while (cursor.getNext()) {
                    cursor.getKey();   // current key
                    var val = cursor.getValue(); // current value

                    var createdTime = Instant.ofEpochSecond(LongBinding.entryToLong(val));
                    if (Duration.between(createdTime, Instant.now()).compareTo(Duration.ofDays(1)) >= 0) {
                        cursor.deleteCurrent();
                    }
                }
            }
        });
    }
}
