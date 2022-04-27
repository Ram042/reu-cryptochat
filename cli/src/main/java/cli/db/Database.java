package cli.db;

import jetbrains.exodus.bindings.ComparableBinding;
import jetbrains.exodus.bindings.LongBinding;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.util.LightOutputStream;
import lib.utils.Crypto;
import lombok.AccessLevel;
import lombok.Getter;
import moe.orangelabs.json.Json;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.HexFormat;

import static java.nio.file.StandardOpenOption.*;
import static moe.orangelabs.json.Json.object;

public class Database implements Closeable {
    public static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 10_000;

    @Getter(AccessLevel.PACKAGE)
    private final Environment env;
    private final PersistentEntityStore store;
    @Getter
    private final Profiles profiles;
    @Getter
    private final Users users;
    @Getter
    private final Sessions sessions;
    @Getter
    private final Messages messages;

    /**
     * @param dbPath
     * @param password
     * @throws IOException
     */
    public Database(Path dbPath, String password) throws Exception {
        if (!Files.isDirectory(dbPath)) {
            throw new NotDirectoryException("Database does not exist");
        }

        var crypto = Json.parse(Files.readString(dbPath.resolve("crypto.json"))).getAsObject();

        var key = HexFormat.of().parseHex(crypto.getString("bdEncryptedKey").string);

        var dec = Crypto.Encrypt.decrypt(key,
                Crypto.pbkdf2(password, HexFormat.of().parseHex(crypto.getString("salt").string),
                        PBKDF2_ITERATIONS, KEY_LENGTH),
                HexFormat.of().parseHex(crypto.getString("nonce").string)
        );

        EnvironmentConfig config = new EnvironmentConfig();
        config.setCipherId("jetbrains.exodus.crypto.streamciphers.ChaChaStreamCipherProvider");
        config.setCipherKey(HexFormat.of().formatHex(dec));
        config.setCipherBasicIV(crypto.getNumber("dbIv").longValue());
        env = Environments.newContextualInstance(dbPath.toFile(), config);

        store = PersistentEntityStores.newInstance(env);
        store.executeInExclusiveTransaction(txn -> {
            store.registerCustomPropertyType(txn, Instant.class, new ComparableBinding() {
                @Override
                public Comparable readObject(@NotNull ByteArrayInputStream stream) {
                    long seconds = LongBinding.readCompressed(stream);
                    long nanos = LongBinding.readCompressed(stream);
                    return Instant.ofEpochSecond(seconds, nanos);
                }

                @Override
                public void writeObject(@NotNull LightOutputStream output, @NotNull Comparable object) {
                    LongBinding.writeCompressed(output, ((Instant) object).getEpochSecond());
                    LongBinding.writeCompressed(output, ((Instant) object).getNano());
                }
            });
        });

        profiles = new Profiles(store, this);
        users = new Users(store, this);
        sessions = new Sessions(store, this);
        messages = new Messages(store,this);
    }

    public void addProfile(Profiles.Profile profile) {
        users.addUser(new Users.User(profile.getPublicKey()));
        profiles.addProfile(profile);
    }

    public static void createDatabase(Path path, String password)
            throws Exception {
        if (Files.isDirectory(path)) {
            throw new IllegalStateException(MessageFormat.format(
                    "Database folder {0} already exists",
                    path
            ));
        }

        Files.createDirectories(path);

        SecureRandom random = new SecureRandom();

        //env key params
        byte[] envKey = new byte[256 / 8];
        long envIv;
        {
            final EnvironmentConfig config = new EnvironmentConfig();
            config.setCipherId("jetbrains.exodus.crypto.streamciphers.ChaChaStreamCipherProvider");
            random.nextBytes(envKey);
            envIv = random.nextLong();
            config.setCipherKey(HexFormat.of().formatHex(envKey));
            config.setCipherBasicIV(envIv);
            Environment env = Environments.newInstance(path.toFile(), config);
            env.close();
        }

        //env key encryption
        byte[] passSalt = new byte[32];
        random.nextBytes(passSalt);
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);

        //encrypt key
        var enc = Crypto.Encrypt.encrypt(envKey,
                Crypto.pbkdf2(password, passSalt, PBKDF2_ITERATIONS, KEY_LENGTH),
                nonce);

        Files.writeString(
                path.resolve("crypto.json"),
                object(
                        "iterations", PBKDF2_ITERATIONS,
                        "bdEncryptedKey", HexFormat.of().formatHex(enc),
                        "dbIv", envIv,
                        "salt", HexFormat.of().formatHex(passSalt),
                        "nonce", HexFormat.of().formatHex(nonce)
                ).toString(),
                WRITE, CREATE, TRUNCATE_EXISTING
        );
    }

    public void close() {
        store.close();
        env.close();
    }
}
