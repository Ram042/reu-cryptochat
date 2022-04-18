package lib.message;

import lib.Action;
import lib.Message;
import lib.utils.Crypto;
import moe.orangelabs.protoobj.Obj;
import moe.orangelabs.protoobj.ObjSerializable;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

import static com.google.common.base.Preconditions.checkArgument;
import static moe.orangelabs.protoobj.Obj.map;

public class EnvelopeMessage extends Message {

    final byte[] target;

    final String alg;
    final byte[] nonce;
    final int counter;

    final byte[] payload;

    public EnvelopeMessage(byte[] message) {
        super(message);

        var map = Obj.decode(message).getAsMap();
        target = map.getData("TARGET").getData();
        payload = map.getData("PAYLOAD").getData();

        var params = map.getMap("PARAMS");
        alg = params.getString("ALG").getString();
        nonce = params.getData("NONCE").getData();
        counter = params.getInteger("COUNTER").value.intValueExact();

        checkArgument(alg.equals("ChaCha20"));
        checkArgument(counter == 0);
        checkArgument(nonce.length == 12);
    }

    public EnvelopeMessage(byte[] target, EncryptedMessagePayload messagePayload, byte[] key)
            throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        super(Action.ENVELOPE);
        this.target = target;

        alg = "ChaCha20";

        nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        counter = 0;

        var message = messagePayload.serialize().encode();
        //padding
        int newSize = (message.length / 64) * 64 + 64;
        var newMessage = new byte[newSize];
        System.arraycopy(message, 0, newMessage, 0, message.length);

        payload = Crypto.encrypt(newMessage, key, nonce, counter);
    }

    @Override
    public Obj serialize() {
        return map(
                "ACTION", getAction().toString(),
                "TARGET", getTarget(),
                "PARAMS", map(
                        "ALG", "ChaCha20",
                        "NONCE", nonce,
                        "COUNTER", counter
                ),
                "PAYLOAD", payload
        );
    }

    public byte[] getTarget() {
        return target.clone();
    }

    public EncryptedMessagePayload decrypt(byte[] key) throws InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return new EncryptedMessagePayload(Crypto.decrypt(payload, key, nonce, counter));
    }

    public static class EncryptedMessagePayload implements ObjSerializable {

        private final long counter;
        private final Instant time;
        private final String message;

        public EncryptedMessagePayload(byte[] message) {
            var map = Obj.decode(message).getAsMap();
            counter = map.getInteger("COUNTER").value.longValueExact();
            time = Instant.ofEpochMilli(map.getInteger("TIME").value.longValueExact());
            this.message = map.getString("MESSAGE").getString();
        }

        public EncryptedMessagePayload(long counter, Instant time, String message) {
            this.counter = counter;
            this.time = time;
            this.message = message;
        }

        public EncryptedMessagePayload(String message, long counter) {
            this.message = message;
            this.counter = counter;
            this.time = Instant.now();
        }

        @Override
        public Obj serialize() {
            return map(
                    "COUNTER", counter,
                    "TIME", time.toEpochMilli(),
                    "MESSAGE", message
            );
        }
    }
}
