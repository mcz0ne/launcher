package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = VersionArgumentRuleAction::class)
object VersionArgumentRuleActionSerializer :
    KSerializer<VersionArgumentRuleAction> {
    override val descriptor: SerialDescriptor =
        StringDescriptor

    override fun deserialize(decoder: Decoder): VersionArgumentRuleAction {
        return VersionArgumentRuleAction.valueOf(decoder.decodeString().toUpperCase())
    }

    override fun serialize(encoder: Encoder, obj: VersionArgumentRuleAction) {
        encoder.encodeString(obj.name.toLowerCase())
    }
}