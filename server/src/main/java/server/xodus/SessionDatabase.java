package server.xodus;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.LongBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import lib.SignedMessage;
import lib.message.SessionUpdateMessage;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionDatabase {

    private final Store store;

    public SessionDatabase(Store store) {
        this.store = store;
    }

    private static byte[] generateKey(byte[] target, byte type) {
        byte[] result = new byte[32 + 1];

        System.arraycopy(target, 0, result, 0, 32);
        result[32] = type;

        return result;
    }

    private void addMessage(SignedMessage<SessionUpdateMessage> message) {
        byte[] key = message.getMessage().getTarget();

        byte[] value = ByteBuffer.allocate(Long.BYTES + message.serialize().encode().length)
                .putLong(Instant.now().getEpochSecond())
                .put(message.serialize().encode()).array();

        store.getEnvironment().executeInTransaction(txn ->
                store.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value)));
    }

    public void addSessionInit(SignedMessage<SessionUpdateMessage> message) {
        addMessage(message);
    }

    public void addSessionResponse(SignedMessage<SessionUpdateMessage> message) {
        addMessage(message);
    }

    public SignedMessage<SessionUpdateMessage>[] getSessionUpdates(byte[] target) {
        var l = store.getEnvironment().computeInReadonlyTransaction(txn -> {
            try (Cursor cursor = store.openCursor(txn)) {
                final ByteIterable first = cursor.getSearchKey(new ArrayByteIterable(target));
                if (first != null) {
                    ArrayList<SignedMessage<SessionUpdateMessage>> list = new ArrayList<>();

                    list.add(new SignedMessage<>(
                            Arrays.copyOfRange(
                                    first.getBytesUnsafe(),
                                    Long.BYTES,
                                    first.getBytesUnsafe().length)
                    ));

                    while (cursor.getNextDup()) {
                        var arr = new ArrayByteIterable(cursor.getValue());
                        list.add(new SignedMessage<>(
                                Arrays.copyOfRange(
                                        arr.getBytesUnsafe(),
                                        Long.BYTES,
                                        arr.getBytesUnsafe().length)
                        ));
                    }
                    return list;
                } else {
                    return List.of();
                }
            }
        });
        return l.toArray(new SignedMessage[0]);
    }

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
