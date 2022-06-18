package lib.message;

import lib.Action;
import lib.Message;
import lib.utils.Crypto;
import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.ObjSerializable;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkArgument;
import static moe.orangelabs.protoobj.Obj.map;

public class EnvelopeMessage extends Message {

    final String sessionId;

    final byte[] target;

    final String alg;
    final byte[] nonce;

    final byte[] payload;

    public EnvelopeMessage(byte[] message) {
        super(message);

        var map = Obj.decode(message).getAsMap();
        sessionId = map.getString("SESSION_ID").getString();
        target = map.getData("TARGET").getData();
        payload = map.getData("PAYLOAD").getData();

        var params = map.getMap("PARAMS");
        alg = params.getString("ALG").getString();
        nonce = params.getData("NONCE").getData();

        checkArgument(alg.equals("ChaCha20-Poly1305"));
        checkArgument(nonce.length == 12);
    }

    public EnvelopeMessage(String sessionId, byte[] target, EnvelopePayload envelopePayload, byte[] key)
            throws GeneralSecurityException {
        super(Action.ENVELOPE);
        this.target = target;
        this.sessionId = sessionId;

        alg = "ChaCha20-Poly1305";

        nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        var message = envelopePayload.serialize().encode();
        //padding
        int newSize = (message.length / 64) * 64 + 64;
        var newMessage = new byte[newSize];
        System.arraycopy(message, 0, newMessage, 0, message.length);

        payload = Crypto.Encrypt.encrypt(newMessage, key, nonce);
    }

    @Override
    public Obj serialize() {
        return map(
                "ACTION", getAction().toString(),
                "SESSION_ID", getSessionId(),
                "TARGET", getTarget(),
                "PARAMS", map(
                        "ALG", alg,
                        "NONCE", nonce
                ),
                "PAYLOAD", payload
        );
    }

    public String getSessionId() {
        return sessionId;
    }

    public byte[] getTarget() {
        return target.clone();
    }

    public EnvelopePayload decrypt(byte[] key) throws GeneralSecurityException {
        return new EnvelopePayload(Crypto.Encrypt.decrypt(payload, key, nonce));
    }

    public static class EnvelopePayload implements ObjSerializable {

        private final Instant time;
        private final String message;

        public EnvelopePayload(byte[] message) {
            var map = Obj.decode(message).getAsMap();
            time = Instant.ofEpochMilli(map.getInteger("TIME").value.longValueExact());
            this.message = map.getString("MESSAGE").getString();
        }

        public EnvelopePayload(Instant time, String message) {
            this.time = time;
            this.message = message;
        }

        public EnvelopePayload(String message) {
            this.message = message;
            this.time = Instant.now();
        }

        @Override
        public Obj serialize() {
            return map(
                    "TIME", time.toEpochMilli(),
                    "MESSAGE", message
            );
        }

        public Instant getTime() {
            return time;
        }

        public String getMessage() {
            return message;
        }
    }
}
