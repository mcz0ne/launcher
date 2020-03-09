package lib.minecraft

import kotlinx.serialization.Serializable
import lib.url
import java.io.File
import java.net.URL

@Serializable
data class VersionLibrary(
    val downloads: VersionLibraryDownloads = VersionLibraryDownloads(),
    val extract: VersionLibraryExtract? = null,
    val name: String,
    val natives: Map<String, String>? = mapOf(),
    val rules: List<VersionArgumentRule>? = listOf(),
    @Serializable(with = URLSerializer::class)
    val url: URL? = null
) {
    fun isAllowed(feature: VersionArgumentFeature, os: VersionArgumentOS): Boolean {
        return rules == null || rules.all { it.allowed(feature, os) }
    }

    val location: String
        get() {
            return if (downloads.artifact?.path != null) {
                downloads.artifact.path
            } else {
                val (domain, pkg, version) = name.split(":")
                return listOf(
                    domain.replace(".", File.separator),
                    pkg,
                    version,
                    "$pkg-$version.jar"
                ).joinToString("/")
            }
        }

    val remoteLocation: URL
        get() {
            return downloads.artifact?.url ?: URL(url ?: "https://libraries.minecraft.net/".url(), location)
        }
}