package app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import lib.Util
import java.io.File
import java.io.InputStream
import java.net.URL

@Serializable
data class LauncherConfig(
    val id: String,
    @SerialName("name")
    private val _name: String? = null,
    @SerialName("definition")
    private val _definition: String,
    val news: String = "https://github.com/mcz0ne/launcher/"
) {
    companion object {
        fun loadFromJAR(): LauncherConfig {
            val stream = LauncherConfig::class.java.getResourceAsStream("/launcher.json")
            val lc = load(stream)
            stream.close()

            return lc
        }

        fun loadFromFile(file: File): LauncherConfig {
            val stream = file.inputStream()
            val lc = load(stream)
            stream.close()

            return lc
        }

        private fun load(stream: InputStream): LauncherConfig {
            val rawJson = stream.bufferedReader().readText()
            return Util.json.parse(serializer(), rawJson)
        }
    }

    val name: String
        get() = _name ?: id

    val url: URL
        get() = URL(_definition)
}