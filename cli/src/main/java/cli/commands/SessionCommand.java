package cli.commands;

import cli.db.Database;
import cli.db.model.Session;
import cli.net.Request;
import com.google.common.base.Preconditions;
import lib.Action;
import lib.SignedMessage;
import lib.message.SessionGetMessage;
import lib.message.SessionInitMessage;
import lib.utils.Base62;
import lombok.extern.slf4j.Slf4j;
import moe.orangelabs.protoobj.Obj;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.util.encoders.Hex;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;


@Command(name = "session", aliases = {"chats"},
        description = "Session management (aka chats)")
@Slf4j
public class SessionCommand {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Option(names = "--ulr", defaultValue = "http://localhost:6060/")
    URI uri;

    @Option(names = "--name", defaultValue = "default")
    String name;
    @Option(names = "--password", interactive = true)
    String password;
    private Session build;

    @Command(name = "init")
    public void init(@CommandLine.Parameters(index = "0", description = "Who to send message to (hex)") String id)
            throws IOException, InterruptedException {
        //generate SESSION_INIT_MESSAGE
        byte[] tmpKey = new byte[32];
        new SecureRandom().nextBytes(tmpKey);

        byte[] seed = new byte[8];
        new SecureRandom().nextBytes(seed);

        byte[] publicKey = new Ed25519PrivateKeyParameters(tmpKey).generatePublicKey().getEncoded();
        SessionInitMessage message = new SessionInitMessage(
                publicKey, Hex.decode(id), seed
        );

        Database db = new Database(name + ".db", password, false);

        //get our key
        var key = db.getKey();

        //sign message
        var signedMessage = new SignedMessage<>(message, key);

        db.getSessionCollection().addSession(Session.builder()
                .target(Hex.decode(id))
                .seed(seed)
                .ephemeralKey(tmpKey)
                .instant(Instant.now())
                .build());

        //send session
        var result = new Request(uri)
                .post("session", Base62.encode(signedMessage.serialize().encode()));

        spec.commandLine().getOut().println(result.body());
    }

    @Command(name = "get", description = "Get incoming sessions")
    public void get(@Option(names = "name", description = "Name of local user", required = true, defaultValue = "default") String name)
            throws SQLException, IOException, InterruptedException {
        Database db = new Database(name + ".db", null, false);
        var key = db.getKey();

        var result = new Request(uri).get(
                "session/" + Base62.encode(new SignedMessage<>(new SessionGetMessage(), key).serialize().encode()),
                null);

        var sessions = Obj.decode(Base62.decodeString(result.body())).getAsArray();

        sessions.forEach(obj -> {
            var sessionMsg = new SignedMessage<SessionInitMessage>(obj.encode());
            if (!sessionMsg.verify(Action.SESSION_INIT)) {
                log.warn("Received bad message");
                return;
            }

            var session = db.getSessionCollection()
                    .getSessionBySeed(sessionMsg.getMessage().getTarget(), sessionMsg.getMessage().getSeed());
            session.setTargetEphemeralPublicKey(sessionMsg.getMessage().getSessionPublicKey());

            db.getSessionCollection().addSession(session);
        });
    }

    @Command(name = "update", description = "Reply to init")
    public void update(@Option(names = "name",
            description = "Name of local user",
            required = true, defaultValue = "default") String name,
                       @Option(names = "target", required = true) String targetString) {
        Database db = new Database(name + ".db", null, false);
        var key = db.getKey();

        byte[] target = Hex.decode(targetString);
        Preconditions.checkArgument(target.length == 32);

        var session = db.getSessionCollection().getLatestSession(target);

        if (session == null) {
            log.error("No sessions for target {}", target);
        }

        if (session.getEphemeralKey() != null) {

        }
    }

}
