package server.xodus;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.LongBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import lib.Message;
import lib.SignedMessage;
import lib.message.SessionInitMessage;
import lib.message.SessionResponseMessage;
import server.db.SessionDatabase;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class XodusSessionDatabase implements SessionDatabase {

    private static final byte INIT = 0;
    private static final byte RESPONSE = 1;

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
        byte[] target;
        byte type;

        if (message instanceof SessionInitMessage) {
            target = ((SessionInitMessage) message).getTarget();
            type = INIT;
        } else {
            target = ((SessionResponseMessage) message).getTarget();
            type = RESPONSE;
        }
        byte[] key = generateKey(target, type);

        byte[] value = ByteBuffer.allocate(Long.BYTES + message.serialize().encode().length)
                .putLong(Instant.now().getEpochSecond())
                .put(message.serialize().encode()).array();

        store.getEnvironment().executeInTransaction(txn ->
                store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value)));
    }

    @Override
    public void addSessionInit(SignedMessage<SessionInitMessage> message) {
        addMessage(message.getMessage());
    }

    @Override
    public void addSessionResponse(SignedMessage<SessionInitMessage> message) {
        addMessage(message.getMessage());
    }

    public SignedMessage<SessionInitMessage>[] getSessionInit(byte[] target) {
        var l = store.getEnvironment().computeInReadonlyTransaction(txn -> {
            try (Cursor cursor = store.openCursor(txn)) {
                final ByteIterable v = cursor.getSearchKey(new ArrayByteIterable(
                        generateKey(target, INIT)
                ));
                if (v != null) {
                    ArrayList<SignedMessage<SessionInitMessage>> list = new ArrayList<>();
                    // there is a value for specified key, the variable v contains the leftmost value
                    while (cursor.getNextDup()) {
                        // this loop traverses all pairs with the same key, values differ on each iteration

                        var value = cursor.getValue();
                        ByteBuffer buf = ByteBuffer.allocate(value.getLength() - 4);
                        var it = value.subIterable(4, value.getLength() - 4).iterator();
                        while (it.hasNext()) {
                            buf.put(it.next());
                        }
                        list.add(new SignedMessage<>(buf.array()));
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
