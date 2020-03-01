package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.net.URL
import java.time.OffsetDateTime

@Serializer(forClass = URL::class)
object URLSerializer : KSerializer<URL?> {
    override val descriptor: SerialDescriptor = StringDescriptor

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

@Serializer(forClass = OffsetDateTime::class)
object DateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor = StringDescriptor

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, obj: OffsetDateTime) {
        encoder.encodeString(obj.toString())
    }
}