package lib.yggdrasil

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val errorMessage: String
)