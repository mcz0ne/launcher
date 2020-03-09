package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class VersionLibraryDownloads(
    val artifact: VersionDownload? = null,
    val classifiers: Map<String, VersionDownload>? = mapOf()
)