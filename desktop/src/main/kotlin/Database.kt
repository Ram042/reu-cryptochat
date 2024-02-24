import lib.Crypto
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.util.*
import kotlin.io.encoding.ExperimentalEncodingApi

object Users : IdTable<String>("users") {
    val publicKey = text("public_key")
        .uniqueIndex()
        .entityId()

    val privateKey = text("private_key")
        .nullable()
        .uniqueIndex()

    override val id: Column<EntityID<String>> = publicKey
}

@OptIn(ExperimentalEncodingApi::class)
class User(id: EntityID<String>) : Entity<String>(id) {
    var publicKey by Users.publicKey.transform(
        { EntityID(Base64.getEncoder().encodeToString(it.bytes), Users) },
        { PublicKey(Base64.getDecoder().decode(it.value)) }
    )

    var privateKey by Users.privateKey.transform(
        {
            it?.let {
                Base64.getEncoder().encodeToString(it.bytes)
            }
        },
        {
            it?.let {
                PrivateKey(kotlin.io.encoding.Base64.decode(it))
            }
        }
    )

    companion object : EntityClass<String, User>(Users)
}

class PublicKey(
    bytes: ByteArray
) : Comparable<PublicKey> {
    val bytes = bytes.clone()
        get() = field.clone()

    override fun compareTo(other: PublicKey): Int = Arrays.compare(bytes, other.bytes)
}

class PrivateKey(
    bytes: ByteArray
) : Comparable<PrivateKey> {
    val bytes = bytes.clone()
        get() = field.clone()

    val publicKey = PublicKey(Crypto.Sign.generatePublicKey(bytes))

    override fun compareTo(other: PrivateKey): Int = Arrays.compare(bytes, other.bytes)
}


fun newPrivateKey(): PrivateKey = PrivateKey(SecureRandom().generateSeed(256 / 8))

fun getDefaultUser(): User = transaction {
    User.find {
        Users.privateKey.isNotNull()
    }.firstOrNull()
} ?: transaction {
    User.new {
        val key = newPrivateKey()
        privateKey = key
        publicKey = key.publicKey
    }
}


fun getAccounts(): List<User> = transaction {
    User.find {
        Users.privateKey.isNotNull()
    }.toList()
}
