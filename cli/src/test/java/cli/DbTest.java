package cli;

import cli.db.Database;
import cli.db.Profiles;
import cli.db.Sessions;
import cli.db.Users;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import lib.utils.Base16;
import lib.utils.Crypto;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.assertThat;

public class DbTest {

    @Test
    public void testLinkingToEntityMultipleTimes() throws IOException {
        //Test how many links will be saved if we link to same entity multiple times with one link name
        var store = PersistentEntityStores.newInstance(Files.createTempDirectory("cryptochat-test").toFile());

        store.executeInTransaction(txn -> {
            var e1 = txn.newEntity("e");
            e1.setProperty("id", 1);
            var e2 = txn.newEntity("e");
            e2.setProperty("id", 2);

            e1.addLink("l", e2);
            e1.addLink("l", e2);
            e1.addLink("l", e2);
            e1.addLink("l", e2);
            e1.addLink("l", e2);
        });

        AtomicInteger count = new AtomicInteger();

        store.executeInReadonlyTransaction(txn -> {
            var e1 = txn.find("e", "id", 1).getFirst();
            for (var e2 : e1.getLinks("l")) {
                count.addAndGet(1);
                System.out.println(format("Entity {0} linked to entity {1}", e1, e2));
            }
        });

        System.out.println(format("Linked 5 times to same entity. Found {0} links", count));

        assertThat(count).hasValue(1);

        store.close();
    }

    @Test
    public void testDatabaseEncryption() throws Exception {
        String pass = Long.toString(new Random().nextLong());
        var tmpPath = generateDbPath();
        Database.createDatabase(tmpPath, pass);

        new Database(tmpPath, pass).close();
    }

    @NotNull
    public static Path generateDbPath() {
        //assume working dir is build dir
        //creating dbs in dedicated folder
        return Path.of("testdb/cryptochat-test-" + Math.abs(new Random().nextLong()));
    }

    @Test
    public void testSessionSorting() throws Exception {
        var profile = Profiles.Profile.generate();
        var user = new Users.User(Base16.encode(Crypto.Sign.generatePublicKey(Crypto.Sign.generatePrivateKey())));

        Path dbPath = DbTest.generateDbPath();
        Database.createDatabase(dbPath, "abc");
        try (var db = new Database(dbPath, "abc")) {
            db.addProfile(profile);
            db.getUsers().addUser(user);
            var session = db.getSessions();

            session.putSessionInit(profile, user, new Sessions.Session("1").setInit(Instant.now(), Base16.encode(Crypto.Sign.generatePrivateKey())));
            session.putSessionInit(profile, user, new Sessions.Session("2").setInit(Instant.now(), Base16.encode(Crypto.Sign.generatePrivateKey())));
            session.putSessionInit(profile, user, new Sessions.Session("3").setInit(Instant.now(), Base16.encode(Crypto.Sign.generatePrivateKey())));

            assertThat(session.getLatestSession(user)).isNotNull();
            assertThat(session.getLatestSession(user).getSessionId()).isEqualTo("3");
        }
    }
}
