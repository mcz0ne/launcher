package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class VersionArguments(
    var game: List<@Serializable(with = VersionArgumentSerializer::class) VersionArgument> = listOf(),
    val jvm: List<@Serializable(with = VersionArgumentSerializer::class) VersionArgument> = listOf()
)