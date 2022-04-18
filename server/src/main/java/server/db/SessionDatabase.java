package server.db;

import lib.SignedMessage;
import lib.message.SessionInitMessage;
import server.api.Session;

public interface SessionDatabase {

    void addSessionInit(SignedMessage<SessionInitMessage> message);

    void addSessionResponse(SignedMessage<SessionInitMessage> message);

    SignedMessage<SessionInitMessage>[] getSessionInit(byte[] target);

    void prune();

}
