package lib

import org.bouncycastle.jcajce.spec.GOST3410ParameterSpec
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger.ONE
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import kotlin.test.Test

class CryptoTest {

    @Test
    fun listAlgorithms() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())

        Security.getAlgorithms("Cipher")
            .filter {
                it.lowercase().contains("gost")
            }.forEach {
                println(it)
            }

        KeyAgreement.getInstance("ECGOST3410-2012-256")
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testAgreement() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())

        val pairA = KeyPairGenerator.getInstance("ECGOST3410-2012").run {
            initialize(GOST3410ParameterSpec("Tc26-Gost-3410-12-256-paramSetA"))
            generateKeyPair()
        }
        val pairB = KeyPairGenerator.getInstance("ECGOST3410-2012").run {
            initialize(GOST3410ParameterSpec("Tc26-Gost-3410-12-256-paramSetA"))
            generateKeyPair()
        }

        val keyA = kotlin.run {
            val agreement = KeyAgreement.getInstance("ECGOST3410-2012-256")

            agreement.init(pairA.private, UserKeyingMaterialSpec(ONE.toByteArray()))
            agreement.doPhase(pairB.public, true)

            agreement.generateSecret()
        }

        val keyB = kotlin.run {
            val agreement = KeyAgreement.getInstance("ECGOST3410-2012-256")

            agreement.init(pairB.private, UserKeyingMaterialSpec(ONE.toByteArray()))
            agreement.doPhase(pairA.public, true)

            agreement.generateSecret()
        }

        println(keyA.toHexString())
        println(keyB.toHexString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testSignature() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())

        val pair = KeyPairGenerator.getInstance("ECGOST3410-2012").run {
            initialize(GOST3410ParameterSpec("Tc26-Gost-3410-12-256-paramSetA"))
            generateKeyPair()
        }

        val msg = "Hello!".toByteArray()

        val signature = Signature.getInstance("ECGOST3410-2012-256").run {
            initSign(pair.private)

            update(msg)

            sign()
        }

        println(signature.toHexString())

        val verify = Signature.getInstance("ECGOST3410-2012-256").run {
            initVerify(pair.public)

            update(msg)

            verify(signature)
        }

        println(verify)
    }

    @Test
    fun testEncryption() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())

        val msg = "Hello!".toByteArray()

        val key = KeyGenerator.getInstance("GOST3412-2015").generateKey()

        val iv: ByteArray

        val encrypt = Cipher.getInstance("GOST3412-2015/CTR/NOPADDING").run {
            init(Cipher.ENCRYPT_MODE, key)

            update(msg)

            iv = getIV()

            doFinal()
        }

        println(String(msg))
        println(String(encrypt))

        val decrypt = Cipher.getInstance("GOST3412-2015/CTR/NOPADDING").run {
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            update(encrypt)

            doFinal()
        }

        println(String(decrypt))
    }

}