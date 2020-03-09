package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(
    val accessToken: String,
    val clientToken: String,
    val selectedProfile: AuthenticationSelectedProfile,
    val requestUser: Boolean? = true
)