package cli.commands;

import cli.DbTest;
import cli.db.Database;
import cli.db.Profiles;
import cli.db.Sessions;
import cli.db.Users;
import cli.net.Api;
import lib.Action;
import lib.SignedMessage;
import lib.message.SessionUpdateMessage;
import lib.utils.Base16;
import lib.utils.Base62;
import lib.utils.Crypto;
import moe.orangelabs.protoobj.types.ObjArray;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SessionCommandTest {

    @Test
    public void testInitSession() throws Exception {
        var me = Profiles.Profile.generate("default");
        var target = new Users.User(Profiles.Profile.generate().getPublicKey(), "target");

        //setup state
        Path dbPath = DbTest.generateDbPath();
        Database.createDatabase(dbPath, "abc");
        try (var db = new Database(dbPath, "abc")) {
            db.addProfile(me);
            db.getUsers().addUser(target);
        }

        //setup mocks
        var sessionCommand = new SessionCommand();
        var apiMock = mock(Api.class);
        var responseMock = mock(HttpResponse.class);
        when(apiMock.sendSessionUpdate(any())).thenReturn(responseMock);
        when(responseMock.body()).thenReturn("200");

        sessionCommand.password = "abc";
        sessionCommand.api = apiMock;
        sessionCommand.dbPath = dbPath;
        sessionCommand.spec = mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_DEEP_STUBS);
        sessionCommand.name = "default";

        //action
        sessionCommand.init(target.getSigningPublicKey());

        //verify
        var arg = ArgumentCaptor.forClass(SignedMessage.class);
        verify(apiMock).sendSessionUpdate(arg.capture());
        assertThat(arg.getValue().verify(Action.SESSION_UPDATE)).isTrue();
        var msg = ((SessionUpdateMessage) arg.getValue().getMessage());
        assertThat(msg.getTarget()).containsExactly(Base16.decode(target.getSigningPublicKey()));
    }

    @Test
    public void testGettingSessions() throws Exception {
        var profile = Profiles.Profile.generate("default");

        var targetPrivateKey = Crypto.Sign.generatePrivateKey();

        //setup state
        Path dbPath = DbTest.generateDbPath();
        Database.createDatabase(dbPath, "abc");
        try (var db = new Database(dbPath, "abc")) {
            db.addProfile(profile);
        }

        //setup mocks
        var sessionCommand = new SessionCommand();
        var apiMock = mock(Api.class);
        var responseMock = mock(HttpResponse.class);
        when(apiMock.getSession(any())).thenReturn(responseMock);
        when(responseMock.body()).thenReturn(Base62.encode(ObjArray.array(
                new SignedMessage<>(new SessionUpdateMessage(
                        Crypto.Sign.generatePublicKey(targetPrivateKey),
                        Base16.decode(profile.getPublicKey()),
                        UUID.randomUUID().toString()
                ), targetPrivateKey)

        ).encode()));

        sessionCommand.password = "abc";
        sessionCommand.api = apiMock;
        sessionCommand.dbPath = dbPath;
        sessionCommand.spec = mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_DEEP_STUBS);
        sessionCommand.name = "default";

        //action
        sessionCommand.get();

        //verify
        try (var db = new Database(dbPath, "abc")) {
            var target = new Users.User(Base16.encode(Crypto.Sign.generatePublicKey(targetPrivateKey)));

            assertThat(db.getUsers().getUser(target.getSigningPublicKey(), null)).isNotNull();

            assertThat(db.getSessions().getLatestSession(profile, target)).isNotNull();
        }
    }

    @Test
    public void testReplyingSession() throws Exception {
        var profile = Profiles.Profile.generate("default");

        var targetPrivateKey = Crypto.Sign.generatePrivateKey();
        var targetPublicKey = Crypto.Sign.generatePublicKey(targetPrivateKey);

        //setup state
        Path dbPath = DbTest.generateDbPath();
        Database.createDatabase(dbPath, "abc");
        try (var db = new Database(dbPath, "abc")) {
            db.addProfile(profile);
            db.getUsers().addUser(new Users.User(Base16.encode(targetPublicKey)));
            db.getSessions().putSessionResponse(
                    profile,
                    new Users.User(Base16.encode(targetPublicKey)),
                    new Sessions.Session("test").setResponse(
                            Instant.now(),
                            Base16.encode(Crypto.Sign.generatePublicKey(Crypto.Sign.generatePrivateKey()))
                    )
            );
        }

        //setup mocks
        var sessionCommand = new SessionCommand();
        var apiMock = mock(Api.class);
        var responseMock = mock(HttpResponse.class);
        when(apiMock.sendSessionUpdate(any())).thenReturn(responseMock);
        when(responseMock.body()).thenReturn("200");

        sessionCommand.password = "abc";
        sessionCommand.api = apiMock;
        sessionCommand.dbPath = dbPath;
        sessionCommand.spec = mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_DEEP_STUBS);
        sessionCommand.name = "default";

        //action
        sessionCommand.reply(
                Base16.encode(targetPublicKey),
                false
        );

        //verify
        var arg = ArgumentCaptor.forClass(SignedMessage.class);
        verify(apiMock).sendSessionUpdate(arg.capture());
        assertThat(arg.getValue().verify(Action.SESSION_UPDATE)).isTrue();
        var msg = ((SessionUpdateMessage) arg.getValue().getMessage());
        assertThat(msg.getId()).isEqualTo("test");
        try (var db = new Database(dbPath, "abc")) {
            var target = new Users.User(Base16.encode(Crypto.Sign.generatePublicKey(targetPrivateKey)));

            assertThat(db.getUsers().getUser(target.getSigningPublicKey(), null)).isNotNull();

            assertThat(db.getSessions().getLatestSession(profile, target)).isNotNull();
        }
    }

}
