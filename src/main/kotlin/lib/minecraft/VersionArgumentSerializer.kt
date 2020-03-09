package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = VersionArgument::class)
object VersionArgumentSerializer :
    KSerializer<VersionArgument> {
    override val descriptor: SerialDescriptor =
        StringDescriptor
    override fun deserialize(decoder: Decoder): VersionArgument {
        return try {
            VersionArgument.Single.serializer().deserialize(decoder)
        } catch (ex: Exception) {
            try {
                VersionArgument.Complex.serializer().deserialize(decoder)
            } catch (ex: Exception) {
                VersionArgument.Multi.serializer().deserialize(decoder)
            }
        }
    }

    override fun serialize(encoder: Encoder, obj: VersionArgument) {
        when (obj) {
            is VersionArgument.Single -> VersionArgument.Single.serializer().serialize(encoder, obj)
            is VersionArgument.Multi -> VersionArgument.Multi.serializer().serialize(encoder, obj)
            is VersionArgument.Complex -> VersionArgument.Complex.serializer().serialize(encoder, obj)
        }
    }
}