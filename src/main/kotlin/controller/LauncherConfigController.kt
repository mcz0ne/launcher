package controller

import event.LaunchArgsEvent
import event.UpdateEvent
import tornadofx.Controller
import java.io.File
import java.net.URL
import java.util.*

class LauncherConfigController : Controller() {
    var id: String = ""
        private set

    var name: String = ""
        private set

    private var definition: String = ""

    private var folder: String = ""

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
                    File(File(System.getenv("APPDATA")!!), id).toString()
                } else {
                    File(File(System.getProperty("user.dir")), ".$id").toString()
                }
            }
            else -> props.getProperty("folder")
        }
    }

    fun folder(): File {
        return File(folder)
    }

    fun downloadModpack() {
        fire(UpdateEvent(name, URL(definition), folder()))
    }

    init {
        subscribe<UpdateEvent> {
            val args = lib.modpack.install(it.name, it.definition, it.folder)
            fire(LaunchArgsEvent(args))
        }
    }
}