package controller

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import tornadofx.Controller
import java.io.File


class ConfigController : Controller() {
    @Serializable
    data class Configuration(
        var clientToken: String = ""
    )

    companion object {
        private val json = Json(JsonConfiguration.Stable)
    }

    private val launcherConfigController: LauncherConfigController by inject()
    private val appConfig: Configuration

    init {
        appConfig = try {
            val f = File(launcherConfigController.folder(), "config.json")
            val t = f.inputStream().bufferedReader().readText()
            json.parse(Configuration.serializer(), t)
        } catch (e: Exception) {
            Configuration()
        }
    }

    private fun save() {
        val s = json.stringify(Configuration.serializer(), appConfig)
        val f = File(launcherConfigController.folder(), "config.json")
        f.outputStream().bufferedWriter().write(s)
    }

    var clientToken: String
        get() = appConfig.clientToken
        set(v) {
            appConfig.clientToken = v
            save()
        }
}