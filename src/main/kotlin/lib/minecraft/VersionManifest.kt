package lib.minecraft

import kotlinx.serialization.Serializable
import java.net.URL
import java.time.OffsetDateTime

@Serializable
data class VersionManifestLatest(
    val release: String,
    val snapshot: String
)

@Serializable
data class VersionManifestVersion(
    val id: String,
    val type: MinecraftVersionType,
    @Serializable(with = URLSerializer::class)
    val url: URL?,
    @Serializable(with = DateTimeSerializer::class)
    val time: OffsetDateTime,
    @Serializable(with = DateTimeSerializer::class)
    val releaseTime: OffsetDateTime
)

@Serializable
data class VersionManifest(
    val latest: VersionManifestLatest,
    val versions: List<VersionManifestVersion>
)
