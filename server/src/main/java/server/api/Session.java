package server.api;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import lib.Action;
import lib.SignedMessage;
import lib.message.SessionGetMessage;
import lib.message.SessionUpdateMessage;
import lib.utils.Base62;
import moe.orangelabs.protoobj.types.ObjArray;
import server.db.SessionDatabase;

import java.time.Duration;
import java.time.Instant;

public class Session {

    private final SessionDatabase sessionDatabase;

    public Session(SessionDatabase sessionDatabase) {
        this.sessionDatabase = sessionDatabase;
    }

    public void addInit(Context ctx) {
        var m = new SignedMessage<SessionUpdateMessage>(Base62.decodeString(ctx.body()));

        if (m.getAction() != Action.SESSION_UPDATE) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad msg");
            return;
        }

        if (!m.verify()) {
            ctx.status(HttpCode.BAD_REQUEST);
            ctx.result("bad sig");
            return;
        }

        sessionDatabase.addSessionInit(m);
    }


    public void getInit(Context ctx) {
        var m = new SignedMessage<SessionGetMessage>(Base62.decodeString(
                ctx.pathParam("message")
        ));

        if (m.getAction() != Action.SESSION_GET) {
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

        var messages = sessionDatabase.getSessionUpdates(m.getPublicKey());

        ctx.result(Base62.encode(new ObjArray((Object[]) messages).encode()));
    }
}
