package lib

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

object Signer {
    fun sign(privateKey: ByteArray?, data: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateKey))
        signer.update(data, 0, data.size)
        return signer.generateSignature()
    }

    fun getPublicKeyForPrivate(privateKey: ByteArray?): ByteArray {
        return Ed25519PrivateKeyParameters(privateKey).generatePublicKey().encoded
    }


    fun verify(publicKey: ByteArray?, signature: ByteArray?, data: ByteArray): Boolean {
        val signer = Ed25519Signer()
        signer.init(false, Ed25519PublicKeyParameters(publicKey))
        signer.update(data, 0, data.size)
        return signer.verifySignature(signature)
    }
}
