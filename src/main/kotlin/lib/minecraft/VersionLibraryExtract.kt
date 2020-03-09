package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class VersionLibraryExtract(
    val exclude: List<String>
)