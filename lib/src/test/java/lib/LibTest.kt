package lib

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.Test
import java.security.Security
import kotlin.test.BeforeTest

class LibTest {

    @BeforeTest
    fun init() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun testMessage() {
        val getSessions = GetSessions()
        println("Json ${Json.encodeToString(getSessions)}")
        println("PlainText ${String(getSessions.toPlainText().bytes)}")

        val signed = SignedMessage.sign(getSessions, Crypto.Signature.generatePrivateKey())
        println("Signed plaintext ${String(signed.message.bytes)}")
        println("Signed message ${signed.getMessage<GetSessions>()}")
    }

    @Test
    fun testSignature() {
        val key = Crypto.Signature.generatePrivateKey()
        val msg = PlainText("Hello!".toByteArray())

        val signature = Crypto.Signature.sign(key, msg)

        Assertions.assertThat(
            Crypto.Signature.verify(
                key.publicKey(),
                signature,
                msg
            )
        )
    }

    @Test
    fun testAgreement() {
        val keyA = Crypto.KeyAgreement.generatePrivateKey()
        val keyB = Crypto.KeyAgreement.generatePrivateKey()

        val sharedA = kotlin.run {
            Crypto.KeyAgreement.sharedKey(keyA, keyB.publicKey())
        }
        val sharedB = kotlin.run {
            Crypto.KeyAgreement.sharedKey(keyB, keyA.publicKey())
        }

        Assertions.assertThat(sharedA.bytes).isEqualTo(sharedB.bytes)
    }

    @Test
    fun testEncryption() {
        val key = Crypto.Cipher.generateKey()
        val iv = Crypto.Cipher.IV()

        val encrypt = Crypto.Cipher.encrypt(PlainText("Hello".toByteArray()), key, iv)

        val decrypt = Crypto.Cipher.decrypt(encrypt, key, iv)

        Assertions.assertThat(String(decrypt.bytes)).isEqualTo("Hello")
    }


}