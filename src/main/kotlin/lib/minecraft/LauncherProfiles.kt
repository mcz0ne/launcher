package lib.minecraft

import kotlinx.serialization.Serializable
import lib.Util
import java.io.File

@Serializable
data class LauncherProfiles(
    val profiles: Map<String, LauncherProfile>
) {
    companion object {
        fun parse (file: File): LauncherProfiles {
        val content = file.readText()
        return Util.json.parse(serializer(), content)
    }
}
}