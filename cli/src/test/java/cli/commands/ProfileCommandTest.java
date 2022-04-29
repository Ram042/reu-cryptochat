package cli.commands;

import cli.DbTest;
import cli.net.Api;
import lib.Action;
import lib.SignedMessage;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProfileCommandTest {

    @Test
    public void testPublish() throws Exception {
        //setup
        ProfileCommand profileCommand = new ProfileCommand();
        var apiMock = mock(Api.class);
        var responseMock = mock(HttpResponse.class);
        when(apiMock.registerProfile(any())).thenReturn(responseMock);
        when(responseMock.body()).thenReturn("200");

        profileCommand.password = "abc";
        profileCommand.name = "default";
        profileCommand.api = apiMock;
        profileCommand.dbPath = DbTest.generateDbPath();
        profileCommand.spec = mock(CommandLine.Model.CommandSpec.class, Answers.RETURNS_DEEP_STUBS);

        //action
        profileCommand.init();
        profileCommand.publish(null);

        //verify
        var arg = ArgumentCaptor.forClass(SignedMessage.class);

        verify(apiMock).registerProfile(arg.capture());
        assertThat(arg.getValue().verify(Action.USER_REGISTER)).isTrue();

    }

}
