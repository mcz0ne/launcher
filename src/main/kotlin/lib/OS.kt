package lib

import com.sun.jna.platform.win32.KnownFolders
import com.sun.jna.platform.win32.Shell32Util
import java.io.File
import java.io.IOException

enum class OS {
    Windows,
    Linux,
    BSD,
    OSX,
    Other;

    companion object {
        private lateinit var detectedOS: OS

        fun detect(): OS {
            if (!::detectedOS.isInitialized) {
                val osname = System.getProperty("os.name", "generic").toLowerCase()
                detectedOS = when {
                    osname.contains("win") -> Windows
                    osname.contains("mac") || osname.contains("darwin") -> OSX
                    osname.contains("nux") -> Linux
                    osname.contains("bsd") -> BSD
                    else -> Other
                }
            }

            return detectedOS
        }
    }

    val isWin: Boolean
        get() = this == Windows
    val isLinux: Boolean
        get() = this == Linux
    val isBSD: Boolean
        get() = this == BSD
    val isMac: Boolean
        get() = this == OSX
    val isUnkown: Boolean
        get() = this == Other

    fun dataDir(name: String): File {
        val homeDir = System.getProperty("user.home")
        return when (this) {
            Linux, BSD -> (System.getenv("XDG_DATA_HOME") ?: homeDir + File.separator + ".local/share").file()
                .join(name)
            OSX -> homeDir.file().join("Library", "Application Support", name)
            Windows -> Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_RoamingAppData).file().join(name)
            Other -> throw IOException("cant create data dir")
        }
    }
}