package cli;

import cli.commands.DebugCommand;
import cli.commands.MessageCommand;
import cli.commands.SessionCommand;
import cli.commands.ProfileCommand;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Entrypoint for cli.
 */
@CommandLine.Command(name = "cli",
        version = "0.0",
        subcommands = {ProfileCommand.class, DebugCommand.class, SessionCommand.class, MessageCommand.class},mixinStandardHelpOptions = true)
public class Main
        implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        initCrypto();

        System.exit(new CommandLine(new Main()).execute(args));
    }

    private static void initCrypto() {
        Security.setProperty("crypto.policy", "unlimited");
        Security.addProvider(new BouncyCastleProvider());
        checkNotNull(Security.getProvider("SunEC"), "SunEC provider not supported");
        try {
            Signature.getInstance("Ed25519", "SunEC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.error("Ed25519 signature not available. Check your java installation");
            throw new RuntimeException("Ed25519 signature not available", e);
        }
    }

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}
