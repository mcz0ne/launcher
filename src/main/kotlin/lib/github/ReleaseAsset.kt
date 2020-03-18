package lib.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseAsset(
    val url: String,
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadURL: String
)