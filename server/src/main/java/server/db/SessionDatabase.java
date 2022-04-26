package server.db;

import lib.SignedMessage;
import lib.message.SessionUpdateMessage;

public interface SessionDatabase {

    void addSessionInit(SignedMessage<SessionUpdateMessage> message);

    void addSessionResponse(SignedMessage<SessionUpdateMessage> message);

    SignedMessage<SessionUpdateMessage>[] getSessionUpdates(byte[] target);

    void prune();

}
