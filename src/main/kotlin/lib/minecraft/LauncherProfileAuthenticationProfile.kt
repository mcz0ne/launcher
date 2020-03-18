package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LauncherProfileAuthenticationProfile(
    val displayName: String
)