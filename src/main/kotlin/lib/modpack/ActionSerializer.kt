package lib.modpack

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import lib.minecraft.MinecraftVersionType


@Serializer(forClass = Action::class)
object ActionSerializer :
    KSerializer<Action> {
    override val descriptor: SerialDescriptor = StringDescriptor

    override fun deserialize(decoder: Decoder): Action {
        return Action.valueOf(decoder.decodeString().toUpperCase())
    }

    override fun serialize(encoder: Encoder, obj: Action) {
        encoder.encodeString(obj.name.toLowerCase())
    }
}