package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class AssetObject(
    val hash: String,
    val size: Int
)