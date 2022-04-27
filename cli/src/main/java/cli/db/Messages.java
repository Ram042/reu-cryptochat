package cli.db;

import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * We assume
 */
public class Messages {

    private final PersistentEntityStore store;
    private final Database database;

    public Messages(PersistentEntityStore store, Database database) {
        this.store = store;

        this.database = database;
    }

    public void addMessage(Sessions.Session session, Message message) {
        store.executeInTransaction(txn -> {
            var es = txn.find("session", "id", session.getSessionId()).getFirst();
            if (es == null) {
                throw new IllegalStateException("Session not found");
            }

            Entity em = null;
            for (Entity entity : txn.find("message", "time", message.time)) {
                if (entity.getProperty("message").equals(message.getMessage())) {
                    em = entity;
                    break;
                }
            }
            if (em == null) {
                em = txn.newEntity("message");
                em.setProperty("message", message.message);
                em.setProperty("time", message.time);

                em.setLink("session", es);
                es.addLink("message", em);
            }
        });
    }

    public List<Message> getMessages(Profiles.Profile profile, Users.User targetUser) {
        return store.computeInTransaction(txn -> {
            var eu = txn.find("user", "publicKey", targetUser.getSigningPublicKey()).getFirst();
            if (eu == null) {
                throw new IllegalStateException("Unknown user");
            }

            var ep = txn.find("profile", "privateKey", profile.getPrivateKey()).getFirst();
            if (ep == null) {
                throw new IllegalStateException("Unknown user");
            }

            var l = new ArrayList<Message>();

            for (var es : eu.getLinks("session")) {
                if (es.getLink("profile").equals(ep)) {
                    for (Entity em : es.getLinks("message")) {
                        l.add(new Message(
                                (String) em.getProperty("message"),
                                (Instant) em.getProperty("time")
                        ));
                    }
                }
            }

            return l;
        });
    }

    @Data
    public static class Message implements Comparable<Message> {
        @NotNull
        private final String message;
        @NotNull
        private final Instant time;

        @Override
        public int compareTo(@NotNull Messages.Message o) {
            return this.time.compareTo(o.time);
        }
    }

}
