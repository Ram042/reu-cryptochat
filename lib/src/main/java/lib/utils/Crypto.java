package lib.utils;

import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
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

        public static final int PRIVATE_KEY_SIZE = 256;
        public static final int PRIVATE_KEY_ARRAY_SIZE = PRIVATE_KEY_SIZE / 8;
        public static final int PUBLIC_KEY_SIZE = 256;
        public static final int PUBLIC_KEY_ARRAY_SIZE = PRIVATE_KEY_SIZE / 8;

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

    public static final class DH {
        public static byte[] generatePublicKey(byte[] privateKey) {
            return new X25519PrivateKeyParameters(privateKey).generatePublicKey().getEncoded();
        }

        public static byte[] generatePrivateKey() {
            byte[] key = new byte[256 / 8];
            new SecureRandom().nextBytes(key);
            return key;
        }


        public static byte[] generateSharedKey(byte[] privateKey, byte[] publicKey) {
            var privateParams = new X25519PrivateKeyParameters(privateKey);
            var publicParams = new X25519PublicKeyParameters(publicKey);

            var agreement = new X25519Agreement();
            agreement.init(privateParams);

            byte[] result = new byte[32];
            agreement.calculateAgreement(publicParams, result, 0);
            return result;
        }
    }

    public static final class Hash {

        public static byte[] SHA3_256(byte[] in) {
            var digest = new SHA3Digest(256);
            digest.update(in, 0, in.length);
            var out = new byte[256 / 8];
            digest.doFinal(out, 0);
            return out;
        }

        public static byte[] SHA256(byte[] in) {
            SHA256Digest sha = new SHA256Digest();
            sha.update(in, 0, in.length);
            byte[] out = new byte[32];
            sha.doFinal(out, 0);
            return out;
        }

    }

}
