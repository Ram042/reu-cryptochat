package cli.commands;

import cli.db.Database;
import cli.db.model.Message;
import cli.net.Request;
import com.google.common.base.Preconditions;
import lib.message.EnvelopeMessage;
import lib.utils.Base62;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.util.encoders.Hex;
import picocli.CommandLine;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@CommandLine.Command(name = "message", description = "Message management")
public class MessageCommand {

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
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException, InterruptedException {
        var db = new Database(name + ".db", null, false);

        byte[] target = Hex.decode(targetString);
        Preconditions.checkArgument(target.length == 32);

        //TODO find better session
        var session = db.getSessionCollection().getLatestPreparedSession(target);
        if (session == null) {
            log.error("No valid session for target {}", Hex.encode(target));
            return;
        }

        var payload = new EnvelopeMessage.EncryptedMessagePayload(message, 0);
        var enc = new EnvelopeMessage(
                session.getTargetEphemeralPublicKey(),
                payload,
                generateSharedKey(session.getEphemeralKey(), session.getTargetEphemeralPublicKey())
        );

        db.getMessages().insert(Message.builder()
                .message(message)
                .counter(0)
                .target(target)
                .build());

        var result = new Request(uri)
                .post("/message", Base62.encode(enc.serialize().encode()));

        spec.commandLine().getOut().println(result.body());
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
