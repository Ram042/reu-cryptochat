package lib.utils;

import org.bouncycastle.math.ec.rfc8032.Ed25519;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public final class Crypto {

    public final static class Encrypt {

        /**
         * ChaCha20 encrypt
         */
        public static byte[] encrypt(byte[] message, byte[] key, byte[] nonce)
                throws java.security.GeneralSecurityException {
            var cipher = Cipher.getInstance("ChaCha20-Poly1305");

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20-Poly1305"),
                    new IvParameterSpec(nonce));
            return cipher.doFinal(message);
        }

        /**
         * ChaCha20 decrypt
         */
        public static byte[] decrypt(byte[] message, byte[] key, byte[] nonce)
                throws java.security.GeneralSecurityException {
            var cipher = Cipher.getInstance("ChaCha20-Poly1305");

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "ChaCha20-Poly1305"),
                    new IvParameterSpec(nonce));
            return cipher.doFinal(message);
        }
    }

    public final static class Sign {
        public static byte[] generatePublicKey(byte[] privateKey) {
            var pub = new byte[256 / 8];
            Ed25519.generatePublicKey(privateKey, 0, pub, 0);
            return pub;
        }

        public static byte[] generatePrivateKey() {
            byte[] key = new byte[256 / 8];
            new SecureRandom().nextBytes(key);
            return key;
        }
    }

    public static byte[] pbkdf2(String password, byte[] salt, int interations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return factory.generateSecret(new PBEKeySpec(password.toCharArray(), salt,
                interations, keyLength)).getEncoded();
    }

}
