package lib

class Util {
    enum class OS {
        Windows,
        Linux,
        OSX,
        Other;

        companion object {
            fun detect(): OS {
                val osname = System.getProperty("os.name", "generic").toLowerCase()
                return when {
                    osname.contains("win") -> Windows
                    osname.contains("mac") || osname.contains("darwin") -> OSX
                    osname.contains("nux") -> Linux
                    else -> Other
                }
            }
        }
    }
}