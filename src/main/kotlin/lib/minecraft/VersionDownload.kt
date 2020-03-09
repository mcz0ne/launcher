package lib.minecraft

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class VersionDownload(
    val id: String? = null,
    val path: String? = null,
    val sha1: String,
    val size: Int,
    @Serializable(with = URLSerializer::class)
    val url: URL?
)