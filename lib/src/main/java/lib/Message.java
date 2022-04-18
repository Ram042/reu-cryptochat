package lib;

import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.ObjSerializable;

public abstract class Message implements ObjSerializable {

    protected final Action action;

    public Message(Action action) {
        this.action = action;
    }

    public Message(byte[] message) {
        var map = Obj.decode(message).getAsMap();
        action = Action.valueOf(map.getString("ACTION").getString());
    }

    public Action getAction() {
        return action;
    }

    public static Action getAction(byte[] message) {
        var map = Obj.decode(message).getAsMap();
        return Action.valueOf(map.getString("ACTION").getString());
    }
}
