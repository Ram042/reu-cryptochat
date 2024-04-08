package lib

import org.bouncycastle.math.ec.rfc8032.Ed25519
import java.security.SecureRandom

object Sign {
    const val PRIVATE_KEY_SIZE: Int = 256
    const val PRIVATE_KEY_ARRAY_SIZE: Int = PRIVATE_KEY_SIZE / 8
    const val PUBLIC_KEY_SIZE: Int = 256
    const val PUBLIC_KEY_ARRAY_SIZE: Int = PRIVATE_KEY_SIZE / 8

    fun generatePublicKey(privateKey: ByteArray?): ByteArray {
        val pub = ByteArray(256 / 8)
        Ed25519.generatePublicKey(privateKey, 0, pub, 0)
        return pub
    }

    fun generatePrivateKey(): ByteArray {
        val key = ByteArray(256 / 8)
        SecureRandom().nextBytes(key)
        return key
    }
}