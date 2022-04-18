package lib.message;

import org.testng.annotations.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static lib.Action.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageTest {

    @Test
    public void testEncodingAndDecoding() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        var a1 = new UserRegisterMessage();
        var a2 = new UserRegisterMessage(a1.serialize().encode());
        assertThat(a1.getAction()).isEqualTo(USER_REGISTER);
        assertThat(a2.getAction()).isEqualTo(USER_REGISTER);

        var b1 = new SessionInitMessage(new byte[16], new byte[16], new byte[8]);
        var b2 = new SessionInitMessage(b1.serialize().encode());
        assertThat(b1.getAction()).isEqualTo(SESSION_INIT);
        assertThat(b2.getAction()).isEqualTo(SESSION_INIT);

        var c1 = new SessionGetMessage();
        var c2 = new SessionGetMessage(c1.serialize().encode());
        assertThat(c1.getAction()).isEqualTo(SESSION_GET);
        assertThat(c2.getAction()).isEqualTo(SESSION_GET);

        var d1 = new SessionResponseMessage(new byte[16], new byte[16], new byte[8]);
        var d2 = new SessionResponseMessage(d1.serialize().encode());
        assertThat(d1.getAction()).isEqualTo(SESSION_RESPONSE);
        assertThat(d2.getAction()).isEqualTo(SESSION_RESPONSE);

        var e1 = new EnvelopeMessage(new byte[16], new EnvelopeMessage.EncryptedMessagePayload("", 0), new byte[32]);
        var e2 = new EnvelopeMessage(e1.serialize().encode());
        assertThat(e1.getAction()).isEqualTo(ENVELOPE);
        assertThat(e2.getAction()).isEqualTo(ENVELOPE);

        var f1 = new EnvelopeGetMessage();
        var f2 = new EnvelopeGetMessage(f1.serialize().encode());
        assertThat(f1.getAction()).isEqualTo(ENVELOPE_GET);
        assertThat(f2.getAction()).isEqualTo(ENVELOPE_GET);
    }

}
