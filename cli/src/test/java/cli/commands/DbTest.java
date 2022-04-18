package cli.commands;

import cli.db.model.Session;
import cli.db.table.SessionCollection;
import org.dizitart.no2.Nitrite;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dizitart.no2.objects.filters.ObjectFilters.and;
import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

public class DbTest {

    @Test(enabled = false)
    public void testUpdate() {
        var db = Nitrite.builder()
                .openOrCreate();
        var store = db.getRepository(Session.class);

        var target = new byte[32];
        new Random().nextBytes(target);
        var seed = new byte[8];
        new Random().nextBytes(seed);
        var key = new byte[32];
        new Random().nextBytes(key);
        var now = Instant.now();

        var tKey = new byte[32];
        new Random().nextBytes(tKey);

        var s1 = new Session(target, seed, key, null, now);

        store.insert(s1);
        {
            var sa = db.getCollection("cli.model.Session").find().firstOrDefault();
            System.out.println(sa.toString());

            var sb = db.getRepository(Session.class).find(eq("target", Base64.getEncoder().encode(target)))
                    .firstOrDefault();
            System.out.println(sb);
            var sc = db.getRepository(Session.class).find(eq("target", target))
                    .firstOrDefault();
            System.out.println(sc);
        }

        var s2 = store.find().firstOrDefault();
        s2.setTargetEphemeralPublicKey(tKey);

        assertThat(store.find(eq("target", target)).firstOrDefault()).isNotNull();
        assertThat(store.update(and(eq("target", target), eq("seed", seed)), s2).getAffectedCount()).isEqualTo(1);

        var s3 = store.find().firstOrDefault();

        assertThat(s3.getTarget()).containsExactly(target);
        assertThat(s3.getTargetEphemeralPublicKey()).containsExactly(tKey);
    }

    @Test
    public void testCollection() {
        var db = Nitrite.builder()
                .openOrCreate();
        var collection = new SessionCollection(db.getCollection("abc"));

        var target = new byte[32];
        new Random().nextBytes(target);
        var seed = new byte[8];
        new Random().nextBytes(seed);
        var key = new byte[32];
        new Random().nextBytes(key);
        var now = Instant.now();

        var tKey = new byte[32];
        new Random().nextBytes(tKey);

        //generate and save
        var s1 = new Session(target, seed, key, null, now);
        collection.addSession(s1);

        //get
        var s2 = collection.getLatestSession(target);
        assertThat(s2).isNotNull();
        assertThat(s2.getInstant()).isEqualTo(now.truncatedTo(ChronoUnit.MILLIS));

        //update
        s2.setTargetEphemeralPublicKey(tKey);
        collection.addSession(s2);

        //get updated
        var s3 = collection.getLatestSession(target);

        assertThat(s3.getTarget()).containsExactly(target);
        assertThat(s3.getTargetEphemeralPublicKey()).containsExactly(tKey);
        assertThat(s3.getInstant()).isEqualTo(now.truncatedTo(ChronoUnit.MILLIS));
    }

}
