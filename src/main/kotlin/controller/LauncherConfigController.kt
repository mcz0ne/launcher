package controller

import tornadofx.Controller
import java.io.File
import java.util.*

class LauncherConfigController : Controller() {
    var id: String = ""
        private set

    var name: String = ""
        private set

    var definition: String = ""
        private set

    var folder: String = ""
        private set

    var news: String = ""
        private set

    fun fromProperties(props: Properties) {
        id = props.getProperty("id")
        name = props.getProperty("name")
        if (name.isEmpty()) {
            name = id
        }

        definition = props.getProperty("definition")
        news = props.getProperty("news")
        if (news.isEmpty()) {
            news = "https://github.com/mcz0ne/launcher"
        }

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

    fun folder(): File {
        return File(folder)
    }
}