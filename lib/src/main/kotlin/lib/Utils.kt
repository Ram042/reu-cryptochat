package lib

import com.google.common.io.BaseEncoding
import io.seruco.encoding.base62.Base62
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Base16 {
    @JvmStatic
    fun encode(bytes: ByteArray): String {
        return BaseEncoding.base16().lowerCase().encode(bytes)
    }

    @JvmStatic
    fun decode(string: String): ByteArray {
        return BaseEncoding.base16().lowerCase().decode(string)
    }

    @JvmStatic
    fun isValid(string: String): Boolean {
        return BaseEncoding.base16().lowerCase().canDecode(string)
    }
}

object Base62 {
    private val instance: Base62 = Base62.createInstance()

    @JvmStatic
    fun encode(string: String): ByteArray {
        return instance.decode(string.toByteArray(StandardCharsets.US_ASCII))
    }

    @JvmStatic
    fun decode(bytes: ByteArray?): String {
        return String(instance.encode(bytes), StandardCharsets.US_ASCII)
    }
}

@OptIn(ExperimentalEncodingApi::class)
public object ByteArrayStringSerializer : KSerializer<ByteArray> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("byte[]", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ByteArray =
        Base64.decode(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.encode(value))
    }

}
