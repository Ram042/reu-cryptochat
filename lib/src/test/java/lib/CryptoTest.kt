package lib

import org.bouncycastle.jcajce.provider.asymmetric.ecgost12.BCECGOST3410_2012PrivateKey
import org.bouncycastle.jcajce.spec.GOST3410ParameterSpec
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec
import org.bouncycastle.jce.ECGOST3410NamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger.ONE
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import kotlin.test.BeforeTest
import kotlin.test.Test

class CryptoTest {

    @BeforeTest
    fun init() {
        Security.setProperty("crypto.policy", "unlimited")
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun listAlgorithms() {
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
        val pairA = KeyPairGenerator.getInstance("ECGOST3410-2012").run {
            initialize(GOST3410ParameterSpec("Tc26-Gost-3410-12-256-paramSetA"))
            generateKeyPair()
        }
        val pairB = KeyPairGenerator.getInstance("ECGOST3410-2012").run {
            initialize(GOST3410ParameterSpec("Tc26-Gost-3410-12-256-paramSetA"))
            generateKeyPair()
        }

        run {
            KeyFactory.getInstance("ECGOST3410-2012").run {
                val encoded = pairA.private.encoded
                val decoded = PKCS8EncodedKeySpec(encoded)
                val private = generatePrivate(decoded)

                val ecSpec = ECGOST3410NamedCurveTable.getParameterSpec("Tc26-Gost-3410-12-256-paramSetA")

                val Q = ecSpec.g.multiply((private as BCECGOST3410_2012PrivateKey).d)
                val pubSpec = ECPublicKeySpec(Q, ecSpec)
                val public = generatePublic(pubSpec)
                println(pairA.public.encoded)
                println(public.encoded)
            }
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