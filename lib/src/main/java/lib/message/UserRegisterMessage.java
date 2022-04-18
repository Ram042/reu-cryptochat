package lib.message;

import lib.Action;
import lib.Message;
import moe.orangelabs.protoobj.Obj;

public class UserRegisterMessage extends Message {

    public UserRegisterMessage() {
        super(Action.USER_REGISTER);
    }

    public UserRegisterMessage(byte[] message) {
        super(message);
    }

    @Override
    public Obj serialize() {
        return Obj.map("ACTION", getAction().toString());
    }
}
