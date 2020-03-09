package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = MinecraftVersionType::class)
object MinecraftVersionTypeSerializer :
    KSerializer<MinecraftVersionType> {
    override val descriptor: SerialDescriptor =
        StringDescriptor

    override fun deserialize(decoder: Decoder): MinecraftVersionType {
        return MinecraftVersionType.valueOf(decoder.decodeString().toUpperCase())
    }

    override fun serialize(encoder: Encoder, obj: MinecraftVersionType) {
        encoder.encodeString(obj.name.toLowerCase())
    }
}