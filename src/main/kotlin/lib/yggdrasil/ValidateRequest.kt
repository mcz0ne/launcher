package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class ValidateRequest(
    val accessToken: String,
    val clientToken: String?
)