package lib.message;

import lib.Action;
import lib.Message;
import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.types.ObjInteger;

import java.time.Instant;

public class EnvelopeGetMessage extends Message {

    private final ObjInteger time;

    public EnvelopeGetMessage() {
        super(Action.ENVELOPE_GET);
        this.time = Obj.integer(Instant.now().getEpochSecond());
    }

    public EnvelopeGetMessage(byte[] message) {
        super(message);
        time = Obj.decode(message).getAsMap().getInteger("TIME");
    }

    public Instant getTime() {
        return Instant.ofEpochSecond(time.longValue());
    }

    @Override
    public Obj serialize() {
        return Obj.map(
                "ACTION", getAction().toString(),
                "TIME", time
        );
    }

}
