package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.net.URL

@Serializer(forClass = URL::class)
object URLSerializer : KSerializer<URL?> {
    override val descriptor: SerialDescriptor =
        StringDescriptor

    override fun deserialize(decoder: Decoder): URL? {
        val url = decoder.decodeString()
        return if (url.isBlank()) {
            null
        } else {
            URL(url)
        }
    }

    override fun serialize(encoder: Encoder, obj: URL?) {
        encoder.encodeString(obj?.toString() ?: "")
    }
}