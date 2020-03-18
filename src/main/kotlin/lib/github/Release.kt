package lib.github

import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val assets: List<ReleaseAsset>
)