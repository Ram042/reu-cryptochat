package lib;

import lib.message.*;
import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.ObjSerializable;

public class SignedMessage<T extends Message> implements ObjSerializable {

    private final byte[] message;
    private final T parsedMessage;
    private final byte[] publicKey;
    private final byte[] signature;

    public SignedMessage(byte[] message, byte[] publicKey, byte[] signature) {
        this.message = message;
        this.publicKey = publicKey;
        this.signature = signature;

        this.parsedMessage = parseMessage(message);
    }

    public SignedMessage(T message, byte[] privateKey) {
        parsedMessage = message;
        this.message = message.serialize().encode();

        publicKey = Signer.getPublicKeyForPrivate(privateKey);
        signature = Signer.sign(privateKey, this.message);
    }

    private T parseMessage(byte[] message) {
        return switch (Message.getAction(message)) {
            case SESSION_INIT -> (T) new SessionInitMessage(message);
            case USER_REGISTER -> (T) new UserRegisterMessage(message);
            case SESSION_GET -> (T) new SessionGetMessage(message);
            case SESSION_RESPONSE -> (T) new SessionResponseMessage(message);
            case ENVELOPE -> (T) new EnvelopeMessage(message);
            case ENVELOPE_GET -> (T) new EnvelopeGetMessage(message);
        };
    }

    public SignedMessage(byte[] signedMessage) {
        var map = Obj.decode(signedMessage).getAsMap();

        this.message = map.getData("DATA").getData();
        this.publicKey = map.getData("PUBLIC_KEY").getData();
        this.signature = map.getData("SIG").getData();

        this.parsedMessage = parseMessage(message);
    }

    public boolean verify() {
        return Signer.verify(publicKey, signature, message);
    }

    public boolean verify(Action action) {
        return getAction().equals(action) && verify();
    }

    @Override
    public Obj serialize() {
        return Obj.map(
                "PUBLIC_KEY", publicKey,
                "SIG", signature,
                "DATA", message
        );
    }

    public Action getAction() {
        return parsedMessage.getAction();
    }

    public T getMessage() {
        return parsedMessage;
    }

    public byte[] getRawMessage() {
        return message;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getSignature() {
        return signature;
    }
}
