package lib.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Crypto {

    public static byte[] encrypt(byte[] message, byte[] key, byte[] nonce, int counter)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        var cipher = Cipher.getInstance("ChaCha20");

        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new ChaCha20ParameterSpec(nonce, counter));
        return cipher.doFinal(message);
    }

    public static byte[] decrypt(byte[] message, byte[] key, byte[] nonce, int counter)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        var cipher = Cipher.getInstance("ChaCha20");

        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new ChaCha20ParameterSpec(nonce, counter));
        return cipher.doFinal(message);
    }


}
