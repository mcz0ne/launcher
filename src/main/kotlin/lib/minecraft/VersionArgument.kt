package lib.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class VersionArgumentFeature(
    @SerialName("is_demo_user")
    val isDemoUser: Boolean? = null,
    @SerialName("has_custom_resolution")
    val hasCustomResolution: Boolean? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is VersionArgumentFeature) {
            return super.equals(other)
        }

        if (isDemoUser != null && isDemoUser != other.isDemoUser) {
            return false
        }

        if (hasCustomResolution != null && hasCustomResolution == other.hasCustomResolution) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

@Serializable
data class VersionArgumentOS(
    val name: String? = null,
    val version: String? = null,
    val arch: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is VersionArgumentOS) {
            return super.equals(other)
        }

        if (name != null && name != other.name) {
            return false
        }

        if (version != null && version == other.version) {
            return false
        }

        if (arch != null && arch == other.arch) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}


@Serializable
data class VersionArgumentRule(
    val action: VersionArgumentRuleAction,
    val features: VersionArgumentFeature? = null,
    val os: VersionArgumentOS? = null
) {
    fun allowed(features: VersionArgumentFeature, os: VersionArgumentOS): Boolean {
        val result = action == VersionArgumentRuleAction.ALLOW

        return if ((this.features == null || this.features == features) && (this.os == null || this.os == os)) {
            result
        } else {
            !result
        }
    }
}


@Serializer(forClass = VersionArgument::class)
object VersionArgumentSerializer : KSerializer<VersionArgument> {
    override val descriptor: SerialDescriptor = StringDescriptor
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
            return if (rules.all { it.allowed(feature, os) }) {
                value.arguments(feature, os)
            } else {
                listOf()
            }
        }
    }
}

@Serializable(with = VersionArgumentRuleActionSerializer::class)
enum class VersionArgumentRuleAction {
    ALLOW,
    DISALLOW
}


@Serializer(forClass = VersionArgumentRuleAction::class)
object VersionArgumentRuleActionSerializer : KSerializer<VersionArgumentRuleAction> {
    override val descriptor: SerialDescriptor = StringDescriptor

    override fun deserialize(decoder: Decoder): VersionArgumentRuleAction {
        return VersionArgumentRuleAction.valueOf(decoder.decodeString().toUpperCase())
    }

    override fun serialize(encoder: Encoder, obj: VersionArgumentRuleAction) {
        encoder.encodeString(obj.name.toLowerCase())
    }
}