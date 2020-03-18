package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LauncherProfileVersion(
    val format: Int = 21,
    val name: String = "2.1.131",
    val profilesFormat: Int = 2
)