package cli.commands;

import cli.db.Database;
import cli.db.Profiles;
import cli.net.Api;
import com.google.common.base.Preconditions;
import lib.SignedMessage;
import lib.message.UserRegisterMessage;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.HexFormat;

@CommandLine.Command(name = "profile", description = "Profile management")
@Slf4j
public final class ProfileCommand {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Option(names = "--name", defaultValue = "default")
    String name;

    @Option(names = "--dbPath", defaultValue = "cryptochat")
    Path dbPath;

    @Option(names = "--password", defaultValue = "DefaultInsecurePassword", interactive = true)
    String password;

    Api api;

    @CommandLine.Command(name = "initDb", hidden = true, description = "Init database without default user")
    public void initDb() throws Exception {
        Database.createDatabase(dbPath, this.password);
        try (var db = new Database(dbPath, this.password)) {
            db.close();
        }
    }

    @CommandLine.Command(name = "init")
    public void init() throws Exception {
        Database.createDatabase(dbPath, this.password);
        try (var db = new Database(dbPath, this.password)) {
            var profile = Profiles.Profile.generate();
            profile.setName("default");
            db.addProfile(profile);
        }
    }

    @CommandLine.Command(name = "list")
    public void listProfiles() throws Exception {
        try (var db = new Database(dbPath, this.password)) {
            db.getProfiles().getProfiles().forEach(profile -> {
                PrintWriter out = spec.commandLine().getOut();
                out.print(profile.getPublicKey());
                if (profile.getName() != null) {
                    out.print(":");
                    out.println(profile.getName());
                } else {
                    out.println();
                }
            });
        }
    }

    @CommandLine.Command(name = "print", description = "Print profile info")
    public void printProfile(@Option(names = "--name") String name,
                             @Option(names = "--key") String key,
                             @Option(names = "--print-secret-key") boolean printSecretKey) throws Exception {
        Profiles.Profile profile;
        try (var db = new Database(dbPath, this.password)) {

            Preconditions.checkArgument(name != null ^ key != null, "Can use both name and key");
            profile = null;
            if (name != null) {
                profile = db.getProfiles().getProfiles().stream()
                        .filter(p -> p.getName() != null && p.getName().equals(name))
                        .findAny().orElse(null);
            }

            if (key != null) {
                profile = db.getProfiles().getProfiles().stream()
                        .filter(p -> p.getPublicKey().equals(key))
                        .findAny().orElse(null);
            }
        }

        StringBuilder result = new StringBuilder();
        if (profile == null) {
            result.append("Profile not found");
        } else {
            result.append("Public key: ").append(profile.getPublicKey())
                    .append(System.getProperty("line.separator"));

            result.append("Private key: ");
            if (printSecretKey) {
                result.append(profile.getPrivateKey());
            } else {
                result.append("*".repeat(Profiles.Profile.PRIVATE_KEY_STRING_LENGTH));
            }
        }

        spec.commandLine().getOut().println(result);
    }

    @CommandLine.Command
    public void publish(@Option(names = "--name") String name,
                        @Option(names = "--key") String publicKey,
                        @Option(names = "--url", defaultValue = "http://localhost:6060/") URI url)
            throws Exception {
        if (api == null) {
            api = new Api(url);
        }
        var db = new Database(dbPath, this.password);

        var profile = db.getProfiles().findProfile(name, publicKey);
        db.close();
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found");
        }

        var key = HexFormat.of().parseHex(profile.getPrivateKey());

        var signed = new SignedMessage<>(new UserRegisterMessage(), key);

        var result = api.registerProfile(signed);

        spec.commandLine().getOut().println(result.body());
    }


}
