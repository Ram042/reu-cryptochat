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

    final byte[] target;

    final String alg;
    final byte[] nonce;

    final byte[] payload;

    public EnvelopeMessage(byte[] message) {
        super(message);

        var map = Obj.decode(message).getAsMap();
        target = map.getData("TARGET").getData();
        payload = map.getData("PAYLOAD").getData();

        var params = map.getMap("PARAMS");
        alg = params.getString("ALG").getString();
        nonce = params.getData("NONCE").getData();

        checkArgument(alg.equals("ChaCha20-Poly1305"));
        checkArgument(nonce.length == 12);
    }

    public EnvelopeMessage(byte[] target, EncryptedMessagePayload messagePayload, byte[] key)
            throws GeneralSecurityException {
        super(Action.ENVELOPE);
        this.target = target;

        alg = "ChaCha20-Poly1305";

        nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        var message = messagePayload.serialize().encode();
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
                "TARGET", getTarget(),
                "PARAMS", map(
                        "ALG", alg,
                        "NONCE", nonce
                ),
                "PAYLOAD", payload
        );
    }

    public byte[] getTarget() {
        return target.clone();
    }

    public EncryptedMessagePayload decrypt(byte[] key) throws GeneralSecurityException {
        return new EncryptedMessagePayload(Crypto.Encrypt.decrypt(payload, key, nonce));
    }

    public static class EncryptedMessagePayload implements ObjSerializable {

        private final Instant time;
        private final String message;

        public EncryptedMessagePayload(byte[] message) {
            var map = Obj.decode(message).getAsMap();
            time = Instant.ofEpochMilli(map.getInteger("TIME").value.longValueExact());
            this.message = map.getString("MESSAGE").getString();
        }

        public EncryptedMessagePayload(Instant time, String message) {
            this.time = time;
            this.message = message;
        }

        public EncryptedMessagePayload(String message) {
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
    }
}
