package lib.minecraft

import kotlinx.serialization.*
import java.net.URL
import java.time.OffsetDateTime

@Serializable
data class VersionArguments(
    val game: List<@Serializable(with = VersionArgumentSerializer::class) VersionArgument>,
    val jvm: List<@Serializable(with = VersionArgumentSerializer::class) VersionArgument>
)

@Serializable
data class VersionAssetIndex(
    val id: String,
    val sha1: String,
    val size: Int,
    val totalSize: Int,
    @Serializable(with = URLSerializer::class)
    val url: URL
)

@Serializable
data class VersionDownload(
    val id: String? = null,
    val path: String? = null,
    val sha1: String,
    val size: Int,
    @Serializable(with = URLSerializer::class)
    val url: URL
)

@Serializable
data class VersionLibraryDownloads(
    val artifact: VersionDownload,
    val classifiers: Map<String, VersionDownload>? = mapOf()
)

@Serializable
data class VersionLibraryExtract(
    val exclude: List<String>
)

@Serializable
data class VersionLibrary(
    val downloads: VersionLibraryDownloads,
    val extract: VersionLibraryExtract? = null,
    val name: String,
    val natives: Map<String, String>? = mapOf(),
    val rules: List<VersionArgumentRule>? = listOf()
) {
    fun isAllowed(feature: VersionArgumentFeature, os: VersionArgumentOS): Boolean {
        return rules == null || rules.all { it.allowed(feature, os) }
    }
}

@Serializable
data class Version(
    val arguments: VersionArguments,
    val assetIndex: VersionAssetIndex,
    val assets: String,
    val downloads: Map<String, VersionDownload>,
    val id: String,
    val libraries: List<VersionLibrary>,
    val logging: Logging,
    val mainClass: String,
    val minimumLauncherVersion: Int,
    @Serializable(with = DateTimeSerializer::class)
    val time: OffsetDateTime,
    @Serializable(with = DateTimeSerializer::class)
    val releaseTime: OffsetDateTime,
    val type: MinecraftVersionType
)