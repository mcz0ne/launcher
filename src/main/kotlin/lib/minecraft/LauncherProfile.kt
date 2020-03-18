package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class LauncherProfile(
    val created: String = "1970-01-01T00:00:00.000Z",
    var icon: String = "Grass",
    var lastUsed: String = "1970-01-01T00:00:00.000Z",
    val lastVersionId: String,
    val name: String,
    val type: String
) {
    companion object {
        val SNAPSHOT = LauncherProfile(
            icon = "Crafting_Table",
            lastVersionId = "latest-snapshot",
            name = "",
            type = "latest-snapshot"
        )

        val RELEASE = LauncherProfile(
            lastVersionId = "latest-release",
            name = "",
            type = "latest-release"
        )
    }
}