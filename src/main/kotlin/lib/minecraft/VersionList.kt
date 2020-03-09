package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class VersionList(
    val latest: VersionListLatest,
    val versions: List<VersionListEntry>
)
