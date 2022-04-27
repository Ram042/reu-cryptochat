package lib.message;

import org.testng.annotations.Test;

import java.security.GeneralSecurityException;
import java.util.UUID;

import static lib.Action.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageTest {

    @Test
    public void testEncodingAndDecoding() throws GeneralSecurityException {
        var a1 = new UserRegisterMessage();
        var a2 = new UserRegisterMessage(a1.serialize().encode());
        assertThat(a1.getAction()).isEqualTo(USER_REGISTER);
        assertThat(a2.getAction()).isEqualTo(USER_REGISTER);

        var b1 = new SessionUpdateMessage(new byte[16], new byte[16], UUID.randomUUID().toString());
        var b2 = new SessionUpdateMessage(b1.serialize().encode());
        assertThat(b1.getAction()).isEqualTo(SESSION_UPDATE);
        assertThat(b2.getAction()).isEqualTo(SESSION_UPDATE);

        var c1 = new SessionGetMessage();
        var c2 = new SessionGetMessage(c1.serialize().encode());
        assertThat(c1.getAction()).isEqualTo(SESSION_GET);
        assertThat(c2.getAction()).isEqualTo(SESSION_GET);

        var e1 = new EnvelopeMessage("test", new byte[16], new EnvelopeMessage.EncryptedMessagePayload(""), new byte[32]);
        var e2 = new EnvelopeMessage(e1.serialize().encode());
        assertThat(e1.getAction()).isEqualTo(ENVELOPE);
        assertThat(e2.getAction()).isEqualTo(ENVELOPE);

        var f1 = new EnvelopeGetMessage();
        var f2 = new EnvelopeGetMessage(f1.serialize().encode());
        assertThat(f1.getAction()).isEqualTo(ENVELOPE_GET);
        assertThat(f2.getAction()).isEqualTo(ENVELOPE_GET);
    }

}
