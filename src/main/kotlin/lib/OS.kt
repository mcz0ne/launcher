package lib

enum class OS {
    Windows,
    Linux,
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
    val isMac: Boolean
        get() = this == OSX
    val isUnkown: Boolean
        get() = this == Other
}