package lib.message;

import lib.Action;
import lib.Message;
import moe.orangelabs.protoobj.Obj;

public class SessionResponseMessage extends Message {

    private final byte[] sessionPublicKey;
    private final byte[] target;
    private final byte[] seed;

    public SessionResponseMessage(byte[] sessionPublicKey, byte[] target, byte[] seed) {
        super(Action.SESSION_RESPONSE);
        this.sessionPublicKey = sessionPublicKey;
        this.target = target;
        this.seed = seed;
    }

    public SessionResponseMessage(byte[] message) {
        super(message);
        var map = Obj.decode(message).getAsMap();

        sessionPublicKey = map.getData("SESSION_PUBLIC_KEY").getData();
        target = map.getData("TARGET").getData();
        seed = map.getData("SEED").getData();
    }

    public byte[] getSessionPublicKey() {
        return sessionPublicKey;
    }

    public byte[] getTarget() {
        return target;
    }

    public byte[] getSeed() {
        return seed;
    }

    @Override
    public Obj serialize() {
        return Obj.map(
                "ACTION", getAction().toString(),
                "SESSION_PUBLIC_KEY", sessionPublicKey,
                "TARGET", target,
                "SEED", seed
        );
    }
}
