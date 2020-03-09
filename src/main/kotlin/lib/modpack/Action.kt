package lib.modpack

import kotlinx.serialization.Serializable

@Serializable(with = ActionSerializer::class)
enum class Action {
    EXTRACT,
    REMOVE,
    IGNORE,
    DOWNLOAD
}