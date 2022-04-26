package server.xodus;

import lib.SignedMessage;
import lib.message.SessionUpdateMessage;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionDatabaseTest {

    private XodusDatabase db;

    @BeforeSuite
    public void initDb() {
        db = new XodusDatabase();
    }

    @BeforeTest
    public void clearDb() {
        db.clear();
        db.initStores();
    }

    @Test
    public void test() {
        var sdb = db.getSessionDatabase();
        sdb.addSessionInit(new SignedMessage<>(new SessionUpdateMessage(
                new byte[32], new byte[32], UUID.randomUUID().toString()
        ), new byte[32]));

        assertThat(sdb.getSessionUpdates(new byte[32])).isNotNull();
    }

}
