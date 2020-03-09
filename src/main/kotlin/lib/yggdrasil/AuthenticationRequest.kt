package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationRequest(
    val agent: AuthenticationAgent = AuthenticationAgent(),
    val username: String,
    val password: String,
    val clientToken: String?,
    val requestUser: Boolean? = true
)