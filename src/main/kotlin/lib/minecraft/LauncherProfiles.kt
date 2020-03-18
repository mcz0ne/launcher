package lib.minecraft

import kotlinx.serialization.Serializable
import lib.Util
import java.io.File
import java.util.*

@Serializable
data class LauncherProfiles(
    val authenticationDatabase: Map<String, LauncherProfileAuthenticationEntry> = mapOf(),
    val clientToken: String = UUID.randomUUID().toString(),
    val launcherVersion: LauncherProfileVersion = LauncherProfileVersion(),
    val profiles: Map<String, LauncherProfile> = mapOf(
        SNAPSHOT_UUID to LauncherProfile.SNAPSHOT,
        RELEASE_UUID to LauncherProfile.RELEASE
    ),
    var selectedUser: LauncherProfileSelectedUser? = null,
    val settings: LauncherProfileSettings = LauncherProfileSettings()
) {
    companion object {
        const val FILENAME = "launcher_profiles.json"
        const val SNAPSHOT_UUID = "4a706c614f55606d703ddcbd344fe998"
        const val RELEASE_UUID = "71ce1525189972ba25f052da25eb9014"

        fun parse(file: File): LauncherProfiles {
            val content = file.readText()
            return Util.json.parse(serializer(), content)
        }
    }

    fun save(file: File) {
        val json = Util.json.stringify(serializer(), this)
        file.outputStream().use {
            it.bufferedWriter().use { bw ->
                bw.write(json)
            }
        }
    }
}