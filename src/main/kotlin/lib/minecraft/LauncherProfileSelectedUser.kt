package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LauncherProfileSelectedUser(
    var account: String = "",
    var profile: String = ""
)