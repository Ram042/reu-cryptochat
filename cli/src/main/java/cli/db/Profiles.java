package cli.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import lib.utils.Base16;
import lib.utils.Crypto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HexFormat;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class Profiles {

    private final PersistentEntityStore store;
    private final Database database;

    public Profiles(PersistentEntityStore store, Database database) {
        this.store = store;

        this.database = database;
    }

    void addProfile(Profile profile) {
        store.executeInTransaction(txn -> {
            var e = txn.find("profile", "privateKey", profile.privateKey).getFirst();

            if (e == null) {
                e = txn.newEntity("profile");
            }

            e.setProperty("privateKey", profile.privateKey);
            if (profile.name == null) {
                e.deleteProperty("name");
            } else {
                e.setProperty("name", profile.name);
            }

            var eu = txn.find("user", "publicKey", profile.getPublicKey()).getFirst();
            eu.setLink("profile", e);
        });
    }

    public Collection<Profile> getProfiles() {
        return store.computeInTransaction(txn -> Streams.stream(txn.getAll("profile"))
                .map(entity -> {
                    Profile profile = null;
                    try {
                        profile = new Profile((String) entity.getProperty("privateKey"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (entity.getProperty("name") != null) {
                        profile.name = (String) entity.getProperty("name");
                    }
                    return profile;
                }).collect(Collectors.toUnmodifiableSet()));
    }

    public Profile findProfile(String string) {
        if (Base16.isValid(string) && Base16.decode(string).length == Crypto.Sign.PUBLIC_KEY_ARRAY_SIZE) {
            return findProfile(null, string);
        } else {
            return findProfile(string, null);
        }
    }

    @Nullable
    public Profile findProfile(String name, String key) {
        Preconditions.checkArgument(name != null ^ key != null, "Cannot use both name and key");
        Profiles.Profile profile = null;
        if (name != null) {
            profile = getProfiles().stream()
                    .filter(p -> p.getName() != null && p.getName().equals(name))
                    .findAny().orElse(null);
        }

        if (key != null) {
            profile = getProfiles().stream()
                    .filter(p -> p.getPublicKey().equals(key))
                    .findAny().orElse(null);
        }

        return profile;
    }

    @Getter
    public static class Profile {
        public static final int PRIVATE_KEY_STRING_LENGTH = 256 / 8 * 2;

        @NotNull
        private final String privateKey;
        @NotNull
        private final String publicKey;
        @Setter
        @Nullable
        private String name;

        public Profile(String privateKey) throws GeneralSecurityException {
            checkNotNull(privateKey, "Private key is null");
            checkArgument(privateKey.length() == PRIVATE_KEY_STRING_LENGTH, "Target is null");
            HexFormat.of().parseHex(privateKey);
            this.privateKey = privateKey.toLowerCase();

            this.publicKey = HexFormat.of().formatHex(Crypto.Sign.generatePublicKey(HexFormat.of().parseHex(privateKey)));
        }

        public static Profile generate() throws GeneralSecurityException {
            var kpg = KeyFactory.getInstance("Ed25519");

            var key = new byte[32];
            new SecureRandom().nextBytes(key);

            return new Profile(HexFormat.of().formatHex(key));
        }

        public static Profile generate(String name) throws GeneralSecurityException {
            var p = generate();
            p.setName(name);
            return p;
        }
    }


}
