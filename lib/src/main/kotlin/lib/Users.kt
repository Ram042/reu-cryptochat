package lib

import kotlinx.serialization.Serializable
import java.security.SecureRandom

@Serializable
data class User(
    val privateKey: PrivateKey = newPrivateKey(),
    val publicKey: PublicKey = privateKey.publicKey,
)

fun newPrivateKey(): PrivateKey = PrivateKey(SecureRandom().generateSeed(256 / 8))