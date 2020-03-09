package lib.minecraft

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class VersionAssetIndex(
    val id: String,
    val sha1: String,
    val size: Int,
    val totalSize: Int,
    @Serializable(with = URLSerializer::class)
    val url: URL?
)