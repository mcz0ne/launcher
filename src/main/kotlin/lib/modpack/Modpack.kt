package lib.modpack

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class File(
    val id: String,
    val url: String,
    val sha1: String? = null,
    val homepage: String? = null,
    val target: String? = null,
    val extract: Boolean = false,
    val ignore: Boolean = false
) {
    fun resolveTarget(): String {
        if (target != null) {
            return target
        }

        return when (java.io.File(URL(url).path).extension) {
            "jar" -> "mods"
            "toml", "cfg", "json", "properties" -> "config"
            else -> "."
        }
    }
}

@Serializable
data class Modpack(
    val minecraft: String,
    val forge: String? = null,
    val multiplayer: String? = null,
    val mainClass: String? = null,
    val files: Map<String, File> = mapOf(),
    val remove: List<String> = listOf() // remove by id, set entry in files as ignore: true to resolve
)