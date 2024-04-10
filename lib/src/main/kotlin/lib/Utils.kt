package lib

import com.google.common.io.BaseEncoding
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
