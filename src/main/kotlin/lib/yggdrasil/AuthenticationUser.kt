package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationUser(
    val id: String,
    val username: String
)