package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class VersionListLatest(
    val release: String,
    val snapshot: String
)