package server.api;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import lib.Action;
import lib.SignedMessage;
import lib.message.EnvelopeGetMessage;
import lib.message.EnvelopeMessage;
import lib.utils.Base62;
import moe.orangelabs.protoobj.types.ObjArray;
import server.db.MessageDatabase;

import java.time.Duration;
import java.time.Instant;

public class Message {
    private final MessageDatabase messageDatabase;

    public Message(MessageDatabase messageDatabase) {
        this.messageDatabase = messageDatabase;
    }

    public void add(Context ctx) {
        byte[] signedMessage = Base62.decodeString(ctx.body());
        var m = new SignedMessage<EnvelopeMessage>(signedMessage);

        if (m.getAction() != Action.ENVELOPE) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad msg");
            return;
        }

        if (!m.verify()) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad sig");
            return;
        }

        messageDatabase.addMessage(m.getMessage().getTarget(), signedMessage);
    }

    public void get(Context ctx) {
        var m = new SignedMessage<EnvelopeGetMessage>(Base62.decodeString(
                ctx.pathParam("message")
        ));

        if (m.getAction() != Action.ENVELOPE_GET) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad msg");
            return;
        }

        if (!m.verify()) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad sig");
            return;
        }

        //time within 10 minutes (+- 5 minutes)
        var time = m.getMessage().getTime();
        if (time.isBefore(Instant.now().minus(Duration.ofMinutes(5))) ||
                time.isAfter(Instant.now().plus(Duration.ofMinutes(5)))
        ) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad time");
            return;
        }

        var messages = messageDatabase.getMessages(m.getPublicKey());

        ctx.result(Base62.encode(new ObjArray(messages).encode()));
    }
}
