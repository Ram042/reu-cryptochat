package cli.commands;

import cli.db.Database;
import cli.db.Profiles;
import cli.db.Sessions;
import cli.db.Users;
import cli.net.Api;
import lib.Action;
import lib.SignedMessage;
import lib.message.SessionGetMessage;
import lib.message.SessionUpdateMessage;
import lib.utils.Base16;
import lib.utils.Base62;
import lib.utils.Crypto;
import lombok.extern.slf4j.Slf4j;
import moe.orangelabs.protoobj.Obj;
import org.bouncycastle.util.encoders.Hex;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;


@Command(name = "session", aliases = {"chats"},
        description = "Session management (aka chats)")
@Slf4j
public final class SessionCommand {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Option(names = "--ulr", defaultValue = "http://localhost:6060/")
    URI uri;

    @Option(names = "--name", defaultValue = "default")
    String name;
    @Option(names = "--password", interactive = true, defaultValue = "DefaultInsecurePassword")
    String password;

    @Option(names = "--dbPath", defaultValue = "cryptochat")
    Path dbPath;

    Api api;

    @Command(name = "init", description = "Init session with target")
    public void init(@CommandLine.Parameters(index = "0", description = "Who to send message to (hex)") String id)
            throws Exception {
        if (api == null) {
            api = new Api(uri);
        }

        //generate SESSION_INIT_MESSAGE
        byte[] privateKey = new byte[32];
        new SecureRandom().nextBytes(privateKey);

        byte[] seed = new byte[8];
        new SecureRandom().nextBytes(seed);

        byte[] publicKey = Crypto.DH.generatePublicKey(privateKey);

        SessionUpdateMessage message = new SessionUpdateMessage(
                publicKey, Hex.decode(id), UUID.randomUUID().toString()
        );

        SignedMessage<SessionUpdateMessage> signedMessage;
        try (Database db = new Database(dbPath, password)) {

            //get our key
            Profiles.Profile profile = db.getProfiles().findProfile(name, null);
            String keyString = profile.getPrivateKey();
            var key = HexFormat.of().parseHex(keyString);

            //sign message
            signedMessage = new SignedMessage<>(message, key);

            db.getUsers().addUser(new Users.User(id));
            db.getSessions().putSessionInit(
                    profile,
                    db.getUsers().getUser(id, null),
                    new Sessions.Session(
                            message.getId(),
                            Instant.now(),
                            HexFormat.of().formatHex(privateKey)
                    )
            );
        }

        //send session
        var result = api.sendSessionUpdate(signedMessage);
        if (result.statusCode() != 200) {
            throw new RuntimeException("Server not working " + result);
        }

        spec.commandLine().getOut().println(result.body());
    }

    @Command(name = "get", description = "Get incoming sessions. Does not replies to inits")
    public void get()
            throws Exception {
        if (api == null) {
            api = new Api(uri);
        }
        try (Database db = new Database(dbPath, password)) {
            var profile = db.getProfiles().findProfile(name, null);
            if (profile == null) {
                throw new IllegalArgumentException("No profile " + name);
            }
            var key = HexFormat.of().parseHex(profile.getPrivateKey());

            var result = api.getSession(new SignedMessage<>(new SessionGetMessage(), key));
            if (result.statusCode() != 200) {
                throw new RuntimeException("Server not working ");
            }

            var sessions = Obj.decode(Base62.decode(result.body())).getAsArray();

            sessions.forEach(obj -> {
                var sessionMsg = new SignedMessage<SessionUpdateMessage>(obj.encode());
                if (!sessionMsg.verify(Action.SESSION_UPDATE)) {
                    LOGGER.info("Received bad message");
                    return;
                }

                db.getUsers().addUser(new Users.User(Base16.encode(sessionMsg.getPublicKey())));
                db.getSessions().putSessionResponse(
                        profile,
                        new Users.User(Base16.encode(sessionMsg.getPublicKey())),
                        new Sessions.Session(sessionMsg.getMessage().getId()).setResponse(
                                Instant.now(),
                                Base16.encode(sessionMsg.getMessage().getSessionPublicKey())
                        )
                );
                LOGGER.info("Received session {} from {}",
                        sessionMsg.getMessage().getId(),
                        Base16.encode(sessionMsg.getPublicKey()));
            });
        }
    }

    @Command(name = "reply", description = "Reply to init")
    public void reply(@CommandLine.Parameters(index = "0") String targetString) throws Exception {
        if (api == null) {
            api = new Api(uri);
        }

        try (Database db = new Database(dbPath, password)) {
            var profile = db.getProfiles().findProfile(name);
            if (profile == null) {
                throw new IllegalArgumentException("No profile");
            }
            var targetUser = db.getUsers().getUser(targetString);
            if (targetUser == null) {
                throw new IllegalArgumentException("User not found");
            }

            var session = db.getSessions().getLatestSession(
                    profile, db.getUsers().getUser(targetString, null)
            );

            if (session == null) {
                throw new IllegalStateException("No session for target");
            }

            if (session.isInit()) {
                throw new IllegalStateException("Session already initialized");
            } else {
                session.setInit(Instant.now(), Base16.encode(Crypto.Sign.generatePrivateKey()));
                db.getSessions().putSessionInit(
                        profile, targetUser, session
                );
                var result = api.sendSessionUpdate(new SignedMessage<>(new SessionUpdateMessage(
                        Base16.decode(session.getSessionPublicKey()),
                        Base16.decode(targetUser.getSigningPublicKey()),
                        session.getSessionId()
                ), Base16.decode(profile.getPrivateKey())));
                if (result.statusCode() != 200) {
                    throw new RuntimeException("Server not working " + result);
                }
                spec.commandLine().getOut().println(result.body());
            }
        }
    }

}
