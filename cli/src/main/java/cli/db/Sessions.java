package cli.db;

import com.google.common.collect.Streams;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import lib.utils.Base16;
import lib.utils.Crypto;
import lombok.Getter;
import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.types.ObjMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.BiFunction;

import static cli.db.Sessions.Session.State.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.BaseEncoding.base64;


public class Sessions {

    private final PersistentEntityStore store;
    private final Database database;

    public Sessions(PersistentEntityStore entityStore, Database database) {
        this.store = entityStore;
        this.database = database;
    }

    private String serializeSession(Session session) {
        return base64().encode(Obj.map(
                "target", getTargetForSession(session),
                "ephemeralKey", session.getSessionPublicKey(),
                "targetEphemeralPublicKey", session.getTargetSessionPublicKey(),
                "instant", session.getInitTime().toEpochMilli()
        ).encode());
    }

    private Session deserializeSession(String data) throws GeneralSecurityException {
        ObjMap map = Obj.decode(base64().decode(data)).getAsMap();

        BiFunction<ObjMap, String, byte[]> f = (m, s) -> {
            if (map.get(s).isNull()) {
                return null;
            } else {
                return map.getData(s).getData();
            }
        };

        return new Session(
                map.getString("id").getString(),
                Instant.ofEpochMilli(map.getInteger("instant").longValue()),
                map.getString("ephemeralKey").getString()
        );

    }

    public String getTargetForSession(Session session) {
        return store.computeInReadonlyTransaction(txn -> {
            var e = txn.find("session", "id", session.getSessionId()).getFirst();
            if (e == null) {
                throw new IllegalStateException("Session does not exist");
            }

            return (String) e.getLink("target").getProperty("publicKey");
        });
    }

    /**
     * Add profile and setup links
     */
    void addSession(Profiles.Profile profile, Users.User target, @NotNull Session session) {
        store.executeInTransaction(txn -> {
            var ep = txn.find("profile", "privateKey", profile.getPrivateKey()).getFirst();
            if (ep == null) {
                throw new IllegalStateException("Profile does not exist");
            }

            var eu = txn.find("user", "publicKey", target.getSigningPublicKey()).getFirst();
            if (eu == null) {
                throw new IllegalStateException("User does not exist");
            }

            var es = txn.find("session", "id", session.getSessionId()).getFirst();
            if (es == null) {
                es = txn.newEntity("session");
            }

            es.setProperty("id", session.getSessionId());

            ep.addLink("session", es);
            es.setLink("profile", ep);

            eu.addLink("session", es);
            es.setLink("target", eu);
        });
    }

    public void putSessionResponse(@NotNull Profiles.Profile profile, Users.User target, @NotNull Session session) {
        if (!session.isResponse()) {
            throw new IllegalArgumentException("Session has no response");
        }

        addSession(profile, target, session);
        store.executeInTransaction(txn -> {
            var es = txn.find("session", "id", session.getSessionId()).getFirst();

            es.setProperty("responseTime", session.responseTime);
            es.setProperty("targetPublicKey", session.targetSessionPublicKey);
        });
    }

    public void putSessionInit(@NotNull Profiles.Profile profile, Users.User target, @NotNull Session session) {
        addSession(profile, target, session);
        store.executeInTransaction(txn -> {
            var es = txn.find("session", "id", session.getSessionId()).getFirst();

            es.setProperty("initTime", session.initTime);
            es.setProperty("sessionPrivateKey", session.getSessionPrivateKey());
        });
    }

    /**
     * Latest session for target.
     * No additional checks
     */
    @Nullable
    public Session getLatestSession(Profiles.Profile profile, Users.User user) {
        String sessionId = store.computeInReadonlyTransaction(txn -> {
            var ue = txn.find("user", "publicKey", user.getSigningPublicKey()).getFirst();
            if (ue == null) {
                throw new IllegalArgumentException("User unknown");
            }

            Entity bestSession = Streams.stream(ue.getLinks("session"))
                    //filter profile
                    .filter(es -> {
                        String privateKey = (String) es.getLink("profile").getProperty("privateKey");
                        String publicKey = Base16.encode(Crypto.Sign.generatePublicKey(Base16.decode(privateKey)));
                        return publicKey.equals(profile.getPublicKey());
                    })
                    //confirm time and public key are always both null or set
                    .filter(e -> e.getProperty("initTime") != null || e.getProperty("responseTime") != null)
                    //find newest session
                    .sorted((o1, o2) -> {
                        //return latest time, checking for null
                        BiFunction<Instant, Instant, Instant> func = (i1, i2) -> {
                            if (i1 == null) {
                                return i2;
                            }
                            if (i2 == null) {
                                return i1;
                            }
                            return i1.compareTo(i2) > 0 ? i1 : i2;
                        };

                        var t1 = func.apply((Instant) o1.getProperty("initTime"), (Instant) o1.getProperty("responseTime"));
                        var t2 = func.apply((Instant) o2.getProperty("initTime"), (Instant) o2.getProperty("responseTime"));

                        return t1.compareTo(t2);
                    })
                    .reduce((o1, o2) -> o2)
                    .orElse(null);
            if (bestSession == null) {
                return null;
            } else {
                return (String) bestSession.getProperty("id");
            }
        });

        if (sessionId == null) {
            return null;
        }

        return getSession(sessionId);
    }

    @Nullable
    public Session getLatestReadySession(Profiles.Profile profile, Users.User user) {
        String sessionId = store.computeInReadonlyTransaction(txn -> {
            var ue = txn.find("user", "publicKey", user.getSigningPublicKey()).getFirst();
            if (ue == null) {
                throw new IllegalArgumentException("User unknown");
            }

            Entity bestSession = Streams.stream(ue.getLinks("session"))
                    //filter profile
                    .filter(es -> {
                        String privateKey = (String) es.getLink("profile").getProperty("privateKey");
                        String publicKey = Base16.encode(Crypto.Sign.generatePublicKey(Base16.decode(privateKey)));
                        return publicKey.equals(profile.getPublicKey());
                    })
                    //filter ready
                    .filter(es -> es.getProperty("initTime") != null && es.getProperty("initTime") != null)
                    //find newest session
                    .sorted((o1, o2) -> {
                        //get latest instant
                        BiFunction<Instant, Instant, Instant> func = (i1, i2) -> i1.compareTo(i2) > 0 ? i1 : i2;

                        var t1 = func.apply((Instant) o1.getProperty("initTime"), (Instant) o1.getProperty("responseTime"));
                        var t2 = func.apply((Instant) o2.getProperty("initTime"), (Instant) o2.getProperty("responseTime"));

                        return t1.compareTo(t2);
                    })
                    .reduce((o1, o2) -> o2)
                    .orElse(null);
            if (bestSession == null) {
                return null;
            } else {
                return (String) bestSession.getProperty("id");
            }
        });

        if (sessionId == null) {
            return null;
        }
        return getSession(sessionId);
    }

    public Session getSession(String id) {
        return store.computeInReadonlyTransaction(txn -> {
            var es = txn.find("session", "id", id).getFirst();
            if (es == null) {
                throw new IllegalArgumentException("Unknown session");
            }

            Session session = new Session(id);
            if (es.getProperty("initTime") != null) {
                session.setInit(
                        (Instant) es.getProperty("initTime"),
                        (String) es.getProperty("sessionPrivateKey")
                );
            }
            if (es.getProperty("responseTime") != null) {
                session.setResponse(
                        (Instant) es.getProperty("responseTime"),
                        (String) es.getProperty("targetPublicKey")
                );
            }
            return session;
        });
    }


    /**
     * Represents session.<br>
     * Has multipel states: <br>
     * * EMPTY - empty. Cannot be saved to db in this state <br>
     * * INIT - initialized locally, no response received <br>
     * * RESPONSE - received init message, no local response <br>
     * * READY - initialized locally and received response
     */
    @Getter
    public static class Session {
        /**
         * String to uniquely identify session
         */
        @NotNull
        private final String sessionId;

        //init
        @Nullable
        private Instant initTime;
        @Nullable
        private String sessionPrivateKey;
        @Nullable
        private String sessionPublicKey;

        //response
        @Nullable
        private Instant responseTime;
        @Nullable
        private String targetSessionPublicKey;

        public Session(@NotNull String sessionId) {
            this.sessionId = sessionId;
        }

        public Session setResponse(@NotNull Instant responseTime, @NotNull String targetSessionPublicKey) {
            if (isResponse()) {
                throw new IllegalStateException("Session response is already set");
            }

            this.responseTime = responseTime;
            this.targetSessionPublicKey = targetSessionPublicKey;

            checkArgument(targetSessionPublicKey.length() == Users.User.USER_PUBLIC_KEY_STRING_LENGTH, "Public kay bad size");
            HexFormat.of().parseHex(targetSessionPublicKey);
            this.targetSessionPublicKey = targetSessionPublicKey.toLowerCase();

            return this;
        }

        public Session setInit(@NotNull Instant initTime, @NotNull String sessionPrivateKey) {
            if (isInit()) {
                throw new IllegalStateException("Session init is already set");
            }

            this.initTime = initTime;
            this.sessionPrivateKey = sessionPrivateKey.toLowerCase();

            checkArgument(sessionPrivateKey.length() == Users.User.USER_PUBLIC_KEY_STRING_LENGTH, "Private kay bad size");
            HexFormat.of().parseHex(sessionPrivateKey);
            this.sessionPublicKey = Base16.encode(Crypto.DH.generatePublicKey(Base16.decode(sessionPrivateKey)));

            return this;
        }

        public Session(@NotNull String sessionId, @NotNull Instant initTime, @NotNull String sessionPrivateKey)
                throws java.security.GeneralSecurityException {
            this.sessionId = sessionId;
            this.initTime = initTime;
            this.sessionPrivateKey = sessionPrivateKey;

            this.sessionPublicKey = Base16.encode(Crypto.DH.generatePublicKey(Base16.decode(sessionPrivateKey)));
        }


        public Session(@NotNull String sessionId, @NotNull String targetSessionPublicKey, @NotNull Instant responseTime)
                throws java.security.GeneralSecurityException {
            this.sessionId = sessionId;
            this.responseTime = responseTime;
            this.targetSessionPublicKey = targetSessionPublicKey;
        }

        public State computeState() {
            if (isInit() && isResponse()) {
                return READY;
            }
            if (isInit()) {
                return INIT;
            }
            if (isResponse()) {
                return RESPONSE;
            }
            return EMPTY;
        }

        public boolean isInit() {
            return initTime != null;

        }

        public boolean isResponse() {
            return responseTime != null;
        }

        public enum State {
            EMPTY, INIT, RESPONSE, READY
        }
    }

}
