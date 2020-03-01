package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LoggingClient(
    val argument: String,
    val file: VersionDownload,
    val type: String
)

@Serializable
data class Logging(
    val client: LoggingClient? = null
)