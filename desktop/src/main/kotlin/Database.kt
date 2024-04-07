
import lib.Crypto
import java.security.SecureRandom


class User(
    val privateKey: PrivateKey = newPrivateKey(),
    val publicKey: PublicKey = privateKey.publicKey,
)


class PublicKey(
    bytes: ByteArray
) {
    val bytes = bytes.clone()
        get() = field.clone()
}

class PrivateKey(
    bytes: ByteArray
) {
    val bytes = bytes.clone()
        get() = field.clone()

    val publicKey = PublicKey(Crypto.Sign.generatePublicKey(bytes))
}


fun newPrivateKey(): PrivateKey = PrivateKey(SecureRandom().generateSeed(256 / 8))
