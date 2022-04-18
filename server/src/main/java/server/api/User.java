package server.api;

import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import moe.orangelabs.protoobj.Obj;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.UserDatabase;

import java.util.Base64;

public class User {
    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);

    private final UserDatabase database;

    public User(UserDatabase database) {
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
            var data = Obj.decode(Base64.getDecoder().decode(ctx.body())).getAsMap();

            var key = data.getData("PUBLIC_KEY").getData();
            var sig = data.getData("SIG").getData();
            var signedData = data.getData("DATA").getData();

            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, new Ed25519PublicKeyParameters(key));
            signer.update(signedData, 0, signedData.length);

            if (!signer.verifySignature(sig)) {
                ctx.status(HttpCode.BAD_REQUEST);
            }

            SHA256Digest sha = new SHA256Digest();
            sha.update(key, 0, key.length);
            byte[] id = new byte[32];
            sha.doFinal(id, 0);

            database.addUser(id, data.encode());

            LOGGER.info("Added user {}", Hex.toHexString(id));

            ctx.result("OK");
            ctx.status(HttpCode.OK);
        } catch (Exception e) {
            ctx.status(HttpCode.BAD_REQUEST);
        }
    }
}
