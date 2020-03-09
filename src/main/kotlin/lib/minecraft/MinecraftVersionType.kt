package lib.minecraft

import kotlinx.serialization.*

@Serializable(with = MinecraftVersionTypeSerializer::class)
enum class MinecraftVersionType {
    SNAPSHOT,
    OLD_BETA,
    OLD_ALPHA,
    RELEASE
}


