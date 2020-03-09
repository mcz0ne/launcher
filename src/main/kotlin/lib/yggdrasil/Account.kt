package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val uuid: String,
    val email: String,
    val username: String,
    val accessToken: String
)