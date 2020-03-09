package lib.minecraft

import kotlinx.serialization.Serializable
import java.net.URL
import java.time.OffsetDateTime

@Serializable
data class VersionListEntry(
    val id: String,
    val type: MinecraftVersionType,
    @Serializable(with = URLSerializer::class)
    val url: URL?
)