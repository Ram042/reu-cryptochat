package cli.commands;

import cli.db.Database;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.util.encoders.Hex;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Path;

@Slf4j
@CommandLine.Command(name = "message", description = "Message management")
public final class MessageCommand {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = "--ulr", defaultValue = "http://localhost:6060/")
    URI uri;

    @CommandLine.Option(names = "--name", defaultValue = "default")
    String name;
    @CommandLine.Option(names = "--password", interactive = true)
    String password;

    @CommandLine.Command(name = "send")
    public void sendMessage(@CommandLine.Option(names = {"target"}, required = true) String targetString,
                            @CommandLine.Parameters() String message)
            throws Exception {
        var db = new Database(Path.of("cryptochat"), password);

        byte[] target = Hex.decode(targetString);
        Preconditions.checkArgument(target.length == 32);

//        //TODO find better session
//        var session = db.getSessionCollection().getLatestPreparedSession(target);
//        if (session == null) {
//            log.error("No valid session for target {}", Hex.encode(target));
//            return;
//        }
//
//        var payload = new EnvelopeMessage.EncryptedMessagePayload(message, 0);
//        var enc = new EnvelopeMessage(
//                session.getTargetEphemeralPublicKey(),
//                payload,
//                generateSharedKey(session.getEphemeralKey(), session.getTargetEphemeralPublicKey())
//        );
//
//        db.getMessages().insert(Message.builder()
//                .message(message)
//                .counter(0)
//                .target(target)
//                .build());
//
//        var result = new Request(uri)
//                .post("/message", Base62.encode(enc.serialize().encode()));

//        spec.commandLine().getOut().println(result.body());
    }

    private byte[] generateSharedKey(byte[] privateKey, byte[] publicKey) {
        var privateParams = new X25519PrivateKeyParameters(privateKey);
        var publicParams = new X25519PublicKeyParameters(publicKey);

        var agreement = new X25519Agreement();
        agreement.init(privateParams);

        byte[] result = new byte[32];
        agreement.calculateAgreement(publicParams, result, 0);
        return result;
    }

}
