package cli.commands;

import cli.db.Database;
import cli.db.Messages;
import cli.net.Api;
import com.google.common.base.Preconditions;
import lib.Action;
import lib.SignedMessage;
import lib.message.EnvelopeGetMessage;
import lib.message.EnvelopeMessage;
import lib.utils.Base16;
import lib.utils.Base62;
import lib.utils.Crypto;
import lombok.extern.slf4j.Slf4j;
import moe.orangelabs.protoobj.Obj;
import org.bouncycastle.util.encoders.Hex;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@CommandLine.Command(name = "message", description = "Message management")
public final class MessageCommand {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = "--ulr", defaultValue = "http://localhost:6060/")
    URI uri;

    @CommandLine.Option(names = "--name", defaultValue = "default")
    String name;
    @CommandLine.Option(names = "--password", defaultValue = "DefaultInsecurePassword", interactive = true)
    String password;

    @CommandLine.Option(names = "--dbPath", defaultValue = "cryptochat")
    Path dbPath;

    Api api;

    @CommandLine.Command(name = "send")
    public void sendMessage(@CommandLine.Option(names = {"--target"}, required = true) String targetString,
                            @CommandLine.Parameters() List<String> message)
            throws Exception {
        if (api == null) {
            api = new Api(uri);
        }

        LOGGER.info("Messages: {}", message);
        message.forEach(s -> LOGGER.info("Message: {}", s));

        try (Database db = new Database(dbPath, password)) {
            var profile = db.getProfiles().findProfile(name);
            if (profile == null) {
                throw new IllegalArgumentException("No profile");
            }

            var targetUser = db.getUsers().getUser(targetString);
            if (targetUser == null) {
                throw new IllegalArgumentException("User not found");
            }

            var session = db.getSessions().getLatestReadySession(profile, targetUser);
            if (session == null) {
                throw new IllegalStateException("No usable sessions");
            }

            var sharedKey = Crypto.DH.generateSharedKey(
                    Base16.decode(session.getSessionPrivateKey()),
                    Base16.decode(session.getTargetSessionPublicKey())
            );

            var msg = new SignedMessage<>(
                    new EnvelopeMessage(
                            session.getSessionId(),
                            Base16.decode(targetUser.getSigningPublicKey()),
                            new EnvelopeMessage.EncryptedMessagePayload(
                                    message.stream().reduce((s1, s2) -> s1 + " " + s2).get()
//                                    message
                            ),
                            sharedKey
                    ),
                    Base16.decode(profile.getPrivateKey())
            );

            spec.commandLine().getOut().println(api.sendMessage(msg).body());
        }

        byte[] target = Hex.decode(targetString);
        Preconditions.checkArgument(target.length == 32);
    }

    @CommandLine.Command(name = "get")
    public void getMessages() throws Exception {
        if (api == null) {
            api = new Api(uri);
        }

        try (Database db = new Database(dbPath, password)) {
            var profile = db.getProfiles().findProfile(name);
            if (profile == null) {
                throw new IllegalArgumentException("No profile");
            }

            var response = api.getMessages(
                    new SignedMessage<>(new EnvelopeGetMessage(),
                            Base16.decode(profile.getPrivateKey())));
            if (response.statusCode() != 200) {
                throw new RuntimeException("Bad result from server " + response.body()+ " " + response);
            }
            var messages = response.body();

            var msgs = Obj.decode((Base62.decode(messages))).getAsArray();

            msgs.forEach(obj -> {
                var sig = new SignedMessage<EnvelopeMessage>(obj.encode());
                sig.verify(Action.ENVELOPE);
                var msg = sig.getMessage();

                var user = db.getUsers().getUser(Base16.encode(sig.getPublicKey()));
                if (user == null) {
                    spec.commandLine().getOut().println("Warning: received message from unknown user");
                } else {
                    var session = db.getSessions().getSession(msg.getSessionId());

                    if (session == null) {
                        spec.commandLine().getOut().println("Warning: no session " + msg.getSessionId());
                    } else {
                        try {
                            var dec = msg.decrypt(Crypto.DH.generateSharedKey(
                                    Base16.decode(session.getSessionPrivateKey()),
                                    Base16.decode(session.getTargetSessionPublicKey())
                            ));
                            Messages.Message message = new Messages.Message(
                                    dec.getMessage(),
                                    dec.getTime()
                            );
                            db.getMessages().addMessage(session, message);
                            spec.commandLine().getOut().println("Received message for session " + msg.getSessionId());
                        } catch (GeneralSecurityException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }
    }

    @CommandLine.Command(name = "show")
    public void showMessages(@CommandLine.Option(names = {"--target"}, required = true) String target) throws Exception {
        try (Database db = new Database(dbPath, password)) {
            var profile = db.getProfiles().findProfile(name);
            if (profile == null) {
                throw new IllegalArgumentException("No profile");
            }

            var targetUser = db.getUsers().getUser(target);
            if (targetUser == null) {
                throw new IllegalArgumentException("User not found");
            }

            db.getMessages().getMessages(profile, targetUser).stream()
                    .sorted()
                    .forEach(m -> spec.commandLine().getOut().println(
                            m.getTime() + " " + m.getMessage()
                    ));
        }
    }

}
