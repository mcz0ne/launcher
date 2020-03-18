package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LauncherProfileSettings(
    val channel: String = "release"
)