package cli.net;

import lib.SignedMessage;
import lib.message.SessionGetMessage;
import lib.message.SessionUpdateMessage;
import lib.message.UserRegisterMessage;
import lib.utils.Base62;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;

public class Api {

    private final Request request;

    public Api(URI uri) {
        request = new Request(uri);
    }

    public HttpResponse<String> registerProfile(SignedMessage<UserRegisterMessage> msg)
            throws IOException, InterruptedException {
        return request.post("user", Base62.encode(msg.serialize().encode()));
    }

    public HttpResponse<String> sendSessionUpdate(SignedMessage<SessionUpdateMessage> msg)
            throws IOException, InterruptedException {
        return request.post("session", Base62.encode(msg.serialize().encode()));
    }

    public HttpResponse<String> getSession(SignedMessage<SessionGetMessage> msg) throws IOException, InterruptedException {
        return request.get("session/" + Base62.encode(msg.serialize().encode()), null);
    }
}
