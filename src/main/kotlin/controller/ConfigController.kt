package controller

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import tornadofx.Controller
import java.io.File
import java.util.*


class ConfigController : Controller() {
    @Serializable
    data class Configuration(
        var clientToken: String = UUID.randomUUID().toString()
    )

    companion object {
        private val json = Json(JsonConfiguration.Stable)
    }

    private val launcherConfigController: LauncherConfigController by inject()
    private val appConfig: Configuration
    private val logger = KotlinLogging.logger {}

    init {
        appConfig = try {
            val f = File(launcherConfigController.folder(), "config.json")
            val t = f.inputStream().bufferedReader().readText()
            json.parse(Configuration.serializer(), t)
        } catch (e: Exception) {
            Configuration()
        }

        save()
    }

    fun save() {
        logger.debug("ensuring config path <{}>", launcherConfigController.folder())
        launcherConfigController.folder().mkdirs()

        logger.debug("saving configuration")
        val s = json.stringify(Configuration.serializer(), appConfig)
        File(launcherConfigController.folder(), "config.json").writeText(s)
    }

    var clientToken: String
        get() = appConfig.clientToken
        set(v) {
            appConfig.clientToken = v
            save()
        }
}