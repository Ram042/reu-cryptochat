package server.xodus;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import lib.SignedMessage;
import lib.message.EnvelopeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageDatabase {

    private final Store store;

    public MessageDatabase(Store store) {
        this.store = store;
    }

    public void addMessage(byte[] target, byte[] message) {
        store.getEnvironment().executeInTransaction(txn -> store.put(txn,
                new ArrayByteIterable(target), new ArrayByteIterable(message)));
    }

    public List<SignedMessage<EnvelopeMessage>> getMessages(byte[] target) {
        return store.getEnvironment().computeInReadonlyTransaction(txn -> {
            var list = new ArrayList<ArrayByteIterable>();

            Cursor cursor = store.openCursor(txn);
            var m = cursor.getSearchKey(new ArrayByteIterable(target));
            if (m != null) {
                list.add(new ArrayByteIterable(m));
//                cursor.deleteCurrent();

                while (cursor.getNextDup()) {
                    list.add(new ArrayByteIterable(cursor.getValue()));
//                    cursor.deleteCurrent();
                }
            }

            return list.stream().map(o -> new SignedMessage<EnvelopeMessage>(o.getBytesUnsafe()))
                    .collect(Collectors.toList());
        });
    }
}
