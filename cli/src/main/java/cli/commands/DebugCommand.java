package cli.commands;

import lib.utils.Crypto;
import moe.orangelabs.protoobj.Obj;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.NamedParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

@CommandLine.Command(name = "debug", hidden = true)
public final class DebugCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCommand.class);

    @CommandLine.Command(name = "listAlgs")
    public void listAlgorithms() {
        Arrays.stream(Security.getProviders()).forEach(provider -> {
            System.out.println(provider.getName());
            provider.stringPropertyNames().stream().sorted().forEach(s -> {
                System.out.print("  ");
                System.out.println(s);
            });
        });
    }

    @CommandLine.Command(name = "ecdhUniform")
    public void ecdhUniformTest() {
        var rand = new SecureRandom();
        final int keys = 100_000;
        var bitCount = new int[256];

        for (int ti = 0; ti < keys; ti++) {
            byte[] s1 = new byte[32];
            rand.nextBytes(s1);


            byte[] s2 = new byte[32];
            rand.nextBytes(s2);
            byte[] p2 = Crypto.DH.generatePublicKey(s2);

            var shared = Crypto.Hash.SHA3_256(Crypto.DH.generateSharedKey(s1, p2));

            var bits = BitSet.valueOf(shared);

            for (int ki = 0; ki < 256; ki++) {
                if (bits.get(ki)) {
                    bitCount[ki]++;
                }
            }
        }

        for (int i = 0; i < bitCount.length; i++) {
            if (bitCount[i] > (keys * 0.55f) || bitCount[i] < (keys * 0.45f)) {
                System.out.println("pos " + i + " prob " + (1f * bitCount[i] / keys));
            }
        }
    }

    @CommandLine.Command(name = "dh")
    public void dhTest() throws NoSuchProviderException {
        byte[] aliceKey = new byte[32];
        byte[] bobKey = new byte[32];
        new SecureRandom().nextBytes(aliceKey);
        new SecureRandom().nextBytes(bobKey);

        var alicePrivate = new X25519PrivateKeyParameters(aliceKey);
        var bobPrivate = new X25519PrivateKeyParameters(bobKey);

        var alicePublic = alicePrivate.generatePublicKey();
        var bobPublic = bobPrivate.generatePublicKey();

        LOGGER.info("Alice Private {}", Hex.toHexString(alicePrivate.getEncoded()));
        LOGGER.info("Alice Public  {}", Hex.toHexString(alicePublic.getEncoded()));

        LOGGER.info("Bob Private   {}", Hex.toHexString(bobPrivate.getEncoded()));
        LOGGER.info("Bob Public    {}", Hex.toHexString(bobPublic.getEncoded()));

        var aliceAgreement = new X25519Agreement();
        var bobAgreement = new X25519Agreement();

        aliceAgreement.init(alicePrivate);
        bobAgreement.init(bobPrivate);

        byte[] aliceResult = new byte[32];
        byte[] bobResult = new byte[32];
        aliceAgreement.calculateAgreement(bobPublic, aliceResult, 0);
        bobAgreement.calculateAgreement(alicePublic, bobResult, 0);

        LOGGER.info("Alice Agreement {}", Hex.toHexString(aliceResult));
        LOGGER.info("Bob Agreement   {}", Hex.toHexString(bobResult));

        if (!Arrays.equals(aliceResult, bobResult)) {
            throw new RuntimeException("Agreed to different keys");
        }
    }

    @CommandLine.Command(name = "testSign")
    public void sign() throws GeneralSecurityException {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        LOGGER.info("Key {}", Hex.toHexString(key));

        var message = Obj.map("message", "Hello, world!!").encode();
        LOGGER.info("Message {}", Hex.toHexString(message));
        checkArgument(message.length != 32, "Test another message");

        {
            var privateKey = KeyFactory.getInstance("Ed25519", "SunEC")
                    .generatePrivate(new EdECPrivateKeySpec(NamedParameterSpec.ED25519, key));

            var signer = Signature.getInstance("Ed25519", "SunEC");
            signer.initSign(privateKey);
            signer.update(message);
            var sig = signer.sign();

            LOGGER.info("Signature 1 {}", Hex.toHexString(sig));
        }

        {
            //IDK how to use default java signature interfaces from java for BC
            //org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi.engineGeneratePrivate only
            //  supports only openssh encoded key or PKCS8
            //however, method for public keys supports raw keys
            //until fixed, probably will user SunEC methods
            var signer = new Ed25519Signer();
            signer.init(true, new Ed25519PrivateKeyParameters(key));
            signer.update(message, 0, message.length);
            var sig = signer.generateSignature();
            LOGGER.info("Signature 2 {}", Hex.toHexString(sig));
        }

        LOGGER.info("Signer 1 {}", Signature.getInstance("Ed25519").getProvider());
        LOGGER.info("Signer 2 {}", Signature.getInstance("ED25519").getProvider());

        LOGGER.info("Signer 1 {}", Signature.getInstance("Ed25519", "SunEC").getParameters());
        LOGGER.info("Signer 2 {}", Signature.getInstance("ED25519", "BC").getParameters());

    }

    @CommandLine.Command(name = "testChaCha20")
    public void testChaCha20() throws GeneralSecurityException {
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);

        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        String message = "Hello, world!";

        var encrypt = Cipher.getInstance("ChaCha20");
        encrypt.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new ChaCha20ParameterSpec(nonce, 0));

        byte[] encryptedMessage = encrypt.doFinal(message.getBytes(UTF_8));

        var decrypt = Cipher.getInstance("ChaCha20");
        decrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new ChaCha20ParameterSpec(nonce, 0));
        byte[] decryptedMessage = decrypt.doFinal(encryptedMessage);

        if (!new String(decryptedMessage).equals(message)) {
            throw new RuntimeException("Decryption failed");
        }
    }

    @CommandLine.Command(name = "testPbeEncryption")
    public void testPbeEncryption() throws GeneralSecurityException {
        var msg = new byte[16];
        new Random().nextBytes(msg);
        var salt = new byte[32];
        new Random().nextBytes(salt);

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_256");

        Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_256");

        var privateKey = keyFactory.generateSecret(new PBEKeySpec("Password".toCharArray(), salt, 100, 256));
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        var enc = cipher.doFinal(msg);

        var publicKey = keyFactory.generateSecret(new PBEKeySpec("Password".toCharArray()));
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        var dec = cipher.doFinal(enc);

        LOGGER.info("msg {}", Hex.toHexString(msg));
        LOGGER.info("enc {}", Hex.toHexString(enc));
        LOGGER.info("dec {}", Hex.toHexString(dec));
    }

    @CommandLine.Command(name = "testPbkdf2Speed")
    public void testPbkdf2Speed() throws GeneralSecurityException {
        var pass = "Password".toCharArray();
        var salt = new byte[32];

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        Consumer<Integer> func = iter -> {
            var start = Instant.now();
            try {
                keyFactory.generateSecret(new PBEKeySpec(pass, salt, iter, 256));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            var end = Instant.now();
            Duration dur = Duration.between(start, end);
            System.out.println("Iterations " + iter + " " +
                    "time " + dur.getSeconds() + "s " + dur.toMillisPart() + "ms");
        };

        func.accept(1);
        func.accept(10);
        func.accept(100);
        func.accept(1000);
        func.accept(10000);
        func.accept(100000);
        func.accept(1000000);
        func.accept(10000000);

    }

    @CommandLine.Command(name = "testInstant")
    public void testInstantResolution() {
        //test uniqueness of instants
        {
            var i1 = Instant.now();
            var i2 = Instant.now();
            var i3 = Instant.now();
            var i4 = Instant.now();
            var i5 = Instant.now();

            System.out.println("Raw instant");
            System.out.println("1 " + i1);
            System.out.println("2 " + i2 + " dn " + (i2.getNano() - i1.getNano()));
            System.out.println("3 " + i3 + " dn " + (i3.getNano() - i2.getNano()));
            System.out.println("4 " + i4 + " dn " + (i4.getNano() - i3.getNano()));
            System.out.println("5 " + i5 + " dn " + (i5.getNano() - i4.getNano()));
            System.out.println();
        }

        {
            System.out.println("System nanotime");
            var n1 = System.nanoTime();
            var n2 = System.nanoTime();
            var n3 = System.nanoTime();
            var n4 = System.nanoTime();
            var n5 = System.nanoTime();

            System.out.println("1 " + n1);
            System.out.println("2 " + n2);
            System.out.println("3 " + n3);
            System.out.println("4 " + n4);
            System.out.println("5 " + n5);
            System.out.println();
        }

        {
            var i1 = Instant.now().plusNanos(System.nanoTime() / 100 % 100);
            var i2 = Instant.now().plusNanos(System.nanoTime() / 100 % 100);
            var i3 = Instant.now().plusNanos(System.nanoTime() / 100 % 100);
            var i4 = Instant.now().plusNanos(System.nanoTime() / 100 % 100);
            var i5 = Instant.now().plusNanos(System.nanoTime() / 100 % 100);

            System.out.println("Raw instant + system nanotime");
            System.out.println("1 " + i1);
            System.out.println("2 " + i2 + " dn " + (i2.getNano() - i1.getNano()));
            System.out.println("3 " + i3 + " dn " + (i3.getNano() - i2.getNano()));
            System.out.println("4 " + i4 + " dn " + (i4.getNano() - i3.getNano()));
            System.out.println("5 " + i5 + " dn " + (i5.getNano() - i4.getNano()));
            System.out.println();
        }
    }

}
