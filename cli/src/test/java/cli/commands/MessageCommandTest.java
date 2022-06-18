package cli.commands;

import cli.DbTest;
import cli.db.Database;
import cli.db.Profiles;
import cli.db.Sessions;
import cli.db.Users;
import cli.net.Api;
import lib.Action;
import lib.SignedMessage;
import lib.message.EnvelopeMessage;
import lib.utils.Base16;
import lib.utils.Base62;
import lib.utils.Crypto;
import moe.orangelabs.protoobj.Obj;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MessageCommandTest {

    @Test
    public void testSendingMessage() throws Exception {
        var profile = Profiles.Profile.generate("default");

        var targetPrivateKey = Crypto.Sign.generatePrivateKey();
        var targetPublicKey = Crypto.Sign.generatePublicKey(targetPrivateKey);

        var profileEphemeralPrivateKey = Crypto.DH.generatePrivateKey();
        var targetEphemeralPrivateKey = Crypto.DH.generatePrivateKey();

        //setup state
        Path dbPath = DbTest.generateDbPath();
        Database.createDatabase(dbPath, "abc");
        try (var db = new Database(dbPath, "abc")) {
            db.addProfile(profile);
            db.getUsers().addUser(new Users.User(Base16.encode(targetPublicKey)));
            var session = new Sessions.Session("test")
                    .setInit(
                            Instant.now(),
                            Base16.encode(profileEphemeralPrivateKey)
                    )
                    .setResponse(
                            Instant.now(),
                            Base16.encode(Crypto.DH.generatePublicKey(targetEphemeralPrivateKey))
                    );
            db.getSessions().putSessionInit(
                    profile,
                    new Users.User(Base16.encode(targetPublicKey)),
                    session
            );
            db.getSessions().putSessionResponse(
                    profile,
                    new Users.User(Base16.encode(targetPublicKey)),
                    session
            );
        }

        //setup mocks
        var messageCommand = new MessageCommand();
        var apiMock = mock(Api.class);
        var responseMock = mock(HttpResponse.class);
        when(apiMock.sendMessage(any())).thenReturn(responseMock);
        when(responseMock.body()).thenReturn("200");

        messageCommand.password = "abc";
        messageCommand.api = apiMock;
        messageCommand.dbPath = dbPath;
        messageCommand.spec = mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_DEEP_STUBS);
        messageCommand.name = "default";

        //action
        messageCommand.sendMessage(
                Base16.encode(targetPublicKey),
                List.of("Hello World!")
        );

        //verify
        var arg = ArgumentCaptor.forClass(SignedMessage.class);
        verify(apiMock).sendMessage(arg.capture());

        assertThat(arg.getValue().verify(Action.ENVELOPE)).isTrue();
        var msg = ((EnvelopeMessage) arg.getValue().getMessage());
        assertThat(msg.decrypt(
                Crypto.Hash.SHA3_256(Crypto.DH.generateSharedKey(
                        profileEphemeralPrivateKey,
                        Crypto.DH.generatePublicKey(targetEphemeralPrivateKey)
                ))
        ).getMessage()).isEqualTo("Hello World!");


        try (var db = new Database(dbPath, "abc")) {
            var target = new Users.User(Base16.encode(Crypto.Sign.generatePublicKey(targetPrivateKey)));

            assertThat(db.getUsers().getUser(target.getSigningPublicKey(), null)).isNotNull();

            assertThat(db.getSessions().getLatestSession(profile, target)).isNotNull();
        }
    }

    @Test
    public void testGettingMessages() throws Exception {
        var profile = Profiles.Profile.generate("default");

        var targetPrivateKey = Crypto.Sign.generatePrivateKey();
        var targetPublicKey = Crypto.Sign.generatePublicKey(targetPrivateKey);

        var profileEphemeralPrivateKey = Crypto.DH.generatePrivateKey();
        var targetEphemeralPrivateKey = Crypto.DH.generatePrivateKey();

        //setup state
        Path dbPath = DbTest.generateDbPath();
        Database.createDatabase(dbPath, "abc");
        try (var db = new Database(dbPath, "abc")) {
            db.addProfile(profile);
            db.getUsers().addUser(new Users.User(Base16.encode(targetPublicKey)));
            var session = new Sessions.Session("test")
                    .setInit(
                            Instant.now(),
                            Base16.encode(profileEphemeralPrivateKey)
                    )
                    .setResponse(
                            Instant.now(),
                            Base16.encode(Crypto.DH.generatePublicKey(targetEphemeralPrivateKey))
                    );
            db.getSessions().putSessionInit(
                    profile,
                    new Users.User(Base16.encode(targetPublicKey)),
                    session
            );
            db.getSessions().putSessionResponse(
                    profile,
                    new Users.User(Base16.encode(targetPublicKey)),
                    session
            );
        }

        //setup mocks
        var messageCommand = new MessageCommand();
        var apiMock = mock(Api.class);
        var responseMock = mock(HttpResponse.class);
        when(apiMock.getMessages(any())).thenReturn(responseMock);
        when(responseMock.body()).thenReturn(Base62.encode(Obj.array(
                new SignedMessage<>(
                        new EnvelopeMessage(
                                "test",
                                Base16.decode(profile.getPublicKey()),
                                new EnvelopeMessage.EnvelopePayload(
                                        "Hello World!!"
                                ),
                                Crypto.Hash.SHA3_256(Crypto.DH.generateSharedKey(
                                        profileEphemeralPrivateKey,
                                        Crypto.DH.generatePublicKey(targetEphemeralPrivateKey)
                                ))
                        ),
                        Base16.decode(profile.getPrivateKey())
                )
        ).encode()));
        when(responseMock.statusCode()).thenReturn(200);

        messageCommand.password = "abc";
        messageCommand.api = apiMock;
        messageCommand.dbPath = dbPath;
        messageCommand.spec = mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_DEEP_STUBS);
        messageCommand.name = "default";

        //action
        messageCommand.getMessages();

        //verify
        try (var db = new Database(dbPath, "abc")) {
            var target = new Users.User(Base16.encode(Crypto.Sign.generatePublicKey(targetPrivateKey)));

            var messages = db.getMessages().getMessages(profile, target);
            assertThat(messages).hasSize(1);
            var msg = messages.get(0);
            assertThat(msg.getMessage()).isEqualTo("Hello World!!");

            assertThat(db.getUsers().getUser(target.getSigningPublicKey(), null)).isNotNull();

            assertThat(db.getSessions().getLatestSession(profile, target)).isNotNull();
        }
    }

}
