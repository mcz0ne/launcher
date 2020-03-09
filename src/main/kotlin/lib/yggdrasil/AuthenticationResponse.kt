package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationResponse(
    val accessToken: String,
    val clientToken: String,
    val selectedProfile: AuthenticationSelectedProfile?,
    val user: AuthenticationUser? = null
)