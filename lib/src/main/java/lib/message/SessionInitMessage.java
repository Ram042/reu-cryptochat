package lib.message;

import lib.Action;
import lib.Message;
import moe.orangelabs.protoobj.Obj;

public class SessionInitMessage extends Message {

    private final byte[] sessionPublicKey;
    private final byte[] target;
    private final byte[] seed;

    public SessionInitMessage(byte[] sessionPublicKey, byte[] target, byte[] seed) {
        super(Action.SESSION_INIT);
        this.sessionPublicKey = sessionPublicKey.clone();
        this.target = target.clone();
        this.seed = seed.clone();
    }

    public SessionInitMessage(byte[] message) {
        super(message);
        var map = Obj.decode(message).getAsMap();

        sessionPublicKey = map.getData("SESSION_PUBLIC_KEY").getData();
        target = map.getData("TARGET").getData();
        seed = map.getData("SEED").getData();
    }

    public byte[] getSessionPublicKey() {
        return sessionPublicKey.clone();
    }

    public byte[] getTarget() {
        return target.clone();
    }

    public byte[] getSeed() {
        return seed.clone();
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
