package cli;

import cli.db.Database;
import cli.db.Profiles;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Random;

public class TestProfile {

    @Test
    public void testProfileGeneration() throws Exception {
        Path dbPath = Path.of("cryptotest-test-" + Math.abs(new Random().nextLong()));
        Database.createDatabase(dbPath, "abc");
        var db = new Database(dbPath, "abc");
        db.addProfile(Profiles.Profile.generate());
        db.close();
    }

}
