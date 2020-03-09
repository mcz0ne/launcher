package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor


sealed class VersionArgument {
    abstract fun arguments(feature: VersionArgumentFeature, os: VersionArgumentOS): List<String>

    @Serializable
    data class Single(private val arg: String) : VersionArgument() {
        @Serializer(forClass = Single::class)
        companion object : KSerializer<Single> {
            override val descriptor: SerialDescriptor = StringDescriptor
            override fun deserialize(decoder: Decoder): Single {
                return Single(decoder.decodeString())
            }

            override fun serialize(encoder: Encoder, obj: Single) {
                encoder.encodeString(obj.arg)
            }
        }

        override fun arguments(feature: VersionArgumentFeature, os: VersionArgumentOS): List<String> {
            return listOf(arg)
        }
    }

    @Serializable
    data class Multi(val args: List<String>) : VersionArgument() {
        @Serializer(forClass = Multi::class)
        companion object : KSerializer<Multi> {
            override val descriptor: SerialDescriptor = StringDescriptor
            override fun deserialize(decoder: Decoder): Multi {
                return Multi(String.serializer().list.deserialize(decoder))
            }

            override fun serialize(encoder: Encoder, obj: Multi) {
                String.serializer().list.serialize(encoder, obj.args)
            }
        }

        override fun arguments(feature: VersionArgumentFeature, os: VersionArgumentOS): List<String> {
            return args
        }
    }

    @Serializable
    data class Complex(
        private val rules: List<VersionArgumentRule>,
        @Serializable(with = VersionArgumentSerializer::class)
        private val value: VersionArgument
    ) : VersionArgument() {
        override fun arguments(feature: VersionArgumentFeature, os: VersionArgumentOS): List<String> {
            return if (rules.all {
                    it.allowed(feature, os)
                }) {
                value.arguments(feature, os)
            } else {
                listOf()
            }
        }
    }
}


