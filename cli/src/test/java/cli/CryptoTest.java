package cli;

import cli.commands.DebugCommand;
import lib.utils.Crypto;
import org.testng.annotations.Test;

import java.security.GeneralSecurityException;
import java.security.NoSuchProviderException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

public class CryptoTest {

    @Test
    public void testChaCha20Encryption() throws java.security.GeneralSecurityException {
        new DebugCommand().testChaCha20();
    }

    @Test
    public void testDh() throws NoSuchProviderException {
        new DebugCommand().dhTest();
    }

    @Test
    public void testPbe() throws GeneralSecurityException {
        new DebugCommand().testPbeEncryption();
    }

    @Test
    public void testEncryption() throws GeneralSecurityException {
        var msg = new byte[16];
        var key = new byte[256 / 8];
        var nonce = new byte[96 / 8];

        var enc = Crypto.Encrypt.encrypt(msg, key, nonce);
        {
            //valid
            var dec = Crypto.Encrypt.decrypt(enc, key, nonce);
            assertThat(dec).inHexadecimal().containsExactly(msg);
        }


        //bad message
        {
            var badEnc1 = enc.clone();
            badEnc1[0] += 1;
            catchException(() -> Crypto.Encrypt.decrypt(badEnc1, key, nonce));

            var badEnc2 = msg.clone();
            badEnc2[badEnc2.length - 1] += 1;
            catchException(() -> Crypto.Encrypt.decrypt(badEnc2, key, nonce));
        }

        //bad key
        {
            var badKey = key.clone();
            badKey[0] += 1;
            catchException(() -> Crypto.Encrypt.decrypt(enc, badKey, nonce));
        }

        //bad nonce
        {
            var badNonce = nonce.clone();
            badNonce[0] += 1;
            catchException(() -> Crypto.Encrypt.decrypt(enc, key, badNonce));
        }
    }
}
