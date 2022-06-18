package server.api;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import lib.SignedMessage;
import lib.utils.Base62;
import lib.utils.Crypto;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.xodus.UserDatabase;

public class UserApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserApi.class);

    private final UserDatabase database;

    public UserApi(UserDatabase database) {
        this.database = database;
    }

    public void get(Context ctx) {
        var id = ctx.queryParam("id");

        var user = database.getUser(Hex.decode(id));

        if (user == null) {
            ctx.status(HttpCode.NOT_FOUND);
        } else {
            ctx.result(Hex.encode(user));
            ctx.status(HttpCode.OK);
        }
    }

    public void create(Context ctx) {
        try {
            var msg = new SignedMessage<>(Base62.decode(ctx.body()));

            var id = Crypto.Hash.SHA256(msg.getPublicKey());
            database.addUser(id, msg.serialize().encode());

            LOGGER.info("Added user {}", Hex.toHexString(id));

            ctx.result("OK");
            ctx.status(HttpCode.OK);
        } catch (Exception e) {
            ctx.status(HttpCode.BAD_REQUEST);
        }
    }
}
