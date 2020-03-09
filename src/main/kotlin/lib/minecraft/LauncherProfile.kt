package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LauncherProfile (
    val lastVersionId: String
)