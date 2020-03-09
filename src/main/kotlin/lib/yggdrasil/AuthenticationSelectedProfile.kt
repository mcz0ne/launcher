package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationSelectedProfile(
    val id: String,
    val name: String
)