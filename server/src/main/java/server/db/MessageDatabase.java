package server.db;

import lib.SignedMessage;
import lib.message.EnvelopeMessage;

import java.util.List;

public interface MessageDatabase {

    void addMessage(byte[] target, byte[] message);

    List<SignedMessage<EnvelopeMessage>> getMessages(byte[] target);

}
