package cli.commands;

import cli.db.Database;
import cli.net.Request;
import lib.SignedMessage;
import lib.message.UserRegisterMessage;
import lib.utils.Base62;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

@CommandLine.Command(name = "user", description = "User management")
public final class UserCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCommand.class);

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Option(names = "--name", defaultValue = "default")
    String name;
    @Option(names = "--password", interactive = true)
    String password;

    @CommandLine.Command(name = "create")
    public void generate(@Option(names = "--force") boolean force,
                         @Option(names = "--dry-run") boolean dryRun) throws SQLException {
        var database = new Database(this.name + ".db", this.password, true);
        database.close();
    }

    @CommandLine.Command(name = "print")
    public void printUser(@Option(names = "--print-secret-key") boolean printSecretKey) throws SQLException {

        var db = new Database(this.name + ".db", this.password, false);

        StringBuilder result = new StringBuilder();

        result.append("key: ");
        if (printSecretKey) {
            result.append(Hex.toHexString(db.getKey()));
        } else {
            result.append("*".repeat(64));
        }

        spec.commandLine().getOut().println(result);
    }

    @CommandLine.Command
    public void publish(@Option(names = "--url", defaultValue = "http://localhost:6060/") URI url)
            throws IOException, InterruptedException, SQLException {
        var db = new Database(this.name + ".db", this.password, false);

        var key = db.getKey();

        var signed = new SignedMessage<>(new UserRegisterMessage(), key);

        var result = new Request(url)
                .post("user", Base62.encode(signed.serialize().encode()));

        spec.commandLine().getOut().println(result.body());
    }


}
