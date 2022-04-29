package cli.net;

import lib.SignedMessage;
import lib.message.*;
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

    public HttpResponse<String> getSession(SignedMessage<SessionGetMessage> msg)
            throws IOException, InterruptedException {
        return request.get("session/" + Base62.encode(msg.serialize().encode()), null);
    }

    public HttpResponse<String> sendMessage(SignedMessage<EnvelopeMessage> msg)
            throws IOException, InterruptedException {
        return request.post("message", Base62.encode(msg.serialize().encode()));
    }

    public HttpResponse<String> getMessages(SignedMessage<EnvelopeGetMessage> msg)
            throws IOException, InterruptedException {
        return request.get("message/" + Base62.encode(msg.serialize().encode()), null);
    }
}
