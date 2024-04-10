package lib

import kotlinx.serialization.Serializable
import java.security.SecureRandom

fun randomBytes(count: Int): ByteArray = ByteArray(count).apply { SecureRandom().nextBytes(this) }

interface HasBytes<T> {
    val bytes: ByteArray
}

inline fun <reified T : HasBytes<T>> HasBytes<T>.hasBytesEquals(other: Any?): Boolean {
    if (other !is T) return false
    return bytes.contentEquals(other.bytes)
}

inline fun <reified T : HasBytes<T>> HasBytes<T>.hasBytesHashcode(): Int = bytes.contentHashCode()

@Serializable
data class PlainText(
    @Serializable(with = ByteArrayStringSerializer::class)
    override val bytes: ByteArray
) : HasBytes<PlainText> {
    init {
        require(bytes.isNotEmpty()) { "message must not be empty" }
    }

    override fun equals(other: Any?): Boolean = hasBytesEquals(other)

    override fun hashCode(): Int = bytes.contentHashCode()
}
