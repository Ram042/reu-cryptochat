package cli.db;

import com.google.common.collect.Streams;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import lib.utils.Base16;
import lib.utils.Crypto;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HexFormat;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Users {

    private final PersistentEntityStore store;
    private final Database database;

    public Users(PersistentEntityStore store, Database database) {
        this.store = store;
        this.database = database;
    }

    /**
     * Add user to database.
     * If user.getname() is not null, name in database is updated
     *
     * @param user
     */
    public void addUser(User user) {
        store.executeInTransaction(txn -> {
            var e = txn.find("user", "publicKey", user.getSigningPublicKey()).getFirst();
            if (e == null) {
                e = txn.newEntity("user");
            }

            e.setProperty("publicKey", user.getSigningPublicKey());
            if (user.getName() != null) {
                e.setProperty("name", user.getName());
            }
        });
    }

    public Collection<User> getUsers() {
        return store.computeInTransaction(txn -> Streams.stream(txn.getAll("user"))
                .map(e -> {
                    User user = new User((String) e.getProperty("publicKey"));

                    if (e.getProperty("name") != null) {
                        user.name = (String) e.getProperty("name");
                    }
                    return user;
                }).collect(Collectors.toUnmodifiableSet()));
    }

    public User getUser(String string) {
        if (Base16.isValid(string) && Base16.decode(string).length == Crypto.Sign.PUBLIC_KEY_ARRAY_SIZE) {
            return getUser(string, null);
        } else {
            return getUser(null, string);
        }
    }

    public User getUser(String key, String name) {
        return getUsers().stream()
                .filter(u -> {
                    var keyEquals = u.getSigningPublicKey().equals(key);

                    if (name == null) {
                        return keyEquals;
                    } else {
                        return keyEquals || name.equals(u.getName());
                    }
                })
                .findAny().orElse(null);
    }

    @Getter
    public static class User {
        public static final int USER_PUBLIC_KEY_STRING_LENGTH = 256 / 8 * 2;
        /**
         * Base16 encoded public key
         */
        @NotNull
        private final String signingPublicKey;

        public User(@NotNull String publicKey) {
            checkNotNull(publicKey, "User is null");
            checkArgument(publicKey.length() == USER_PUBLIC_KEY_STRING_LENGTH, "Bad user key");
            HexFormat.of().parseHex(publicKey);
            this.signingPublicKey = publicKey.toLowerCase();
        }

        public User(@NotNull String publicKey, @Nullable String name) {
            this(publicKey);
            this.name = name;
        }

        @Setter
        @Nullable
        private String name;
    }
}
