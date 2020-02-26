package lib

import sun.misc.Launcher
import tornadofx.Controller
import java.io.File
import java.util.*

class LauncherConfig : Controller() {
    var name: String = ""
        private set

    var definition: String = ""
        private set

    var folder: String = ""
        private set

    fun fromProperties(props: Properties) {
        name = props.getProperty("name")
        definition = props.getProperty("definition")

        folder = when (props.getProperty("folder").isNullOrEmpty()) {
            true -> {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    File(File(System.getenv("APPDATA")!!), name).toString()
                } else {
                    File(File(System.getProperty("user.dir")), ".$name").toString()
                }
            }
            else -> props.getProperty("folder")
        }
    }
}