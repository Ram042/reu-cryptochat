package lib.message;

import lib.Action;
import lib.Message;
import moe.orangelabs.protoobj.Obj;

public class SessionUpdateMessage extends Message {

    private final String id;
    private final byte[] sessionPublicKey;
    private final byte[] target;

    public SessionUpdateMessage(byte[] sessionPublicKey, byte[] target, String id) {
        super(Action.SESSION_UPDATE);
        this.sessionPublicKey = sessionPublicKey.clone();
        this.target = target.clone();
        this.id = id;
    }

    public SessionUpdateMessage(byte[] message) {
        super(message);
        var map = Obj.decode(message).getAsMap();

        sessionPublicKey = map.getData("SESSION_PUBLIC_KEY").getData();
        target = map.getData("TARGET").getData();
        id = map.getString("ID").getString();
    }

    public byte[] getSessionPublicKey() {
        return sessionPublicKey.clone();
    }

    public byte[] getTarget() {
        return target.clone();
    }

    public String getId() {
        return id;
    }

    @Override
    public Obj serialize() {
        return Obj.map(
                "ACTION", getAction().toString(),
                "SESSION_PUBLIC_KEY", sessionPublicKey,
                "TARGET", target,
                "ID", id
        );
    }

}
