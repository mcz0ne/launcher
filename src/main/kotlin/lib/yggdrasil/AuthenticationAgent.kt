package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationAgent(
    val name: String = "minecraft",
    val version: Int = 1
)