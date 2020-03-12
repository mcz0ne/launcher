package lib.modpack

import kotlinx.serialization.Serializable
import lib.Util
import mu.KotlinLogging
import java.io.File
import java.net.URL

@Serializable
data class Modpack(
    val minecraft: String,
    val forge: String? = null,

    val multiplayer: String? = null,
    val version: String = "1",
    val packages: List<Package> = listOf()
) {
    companion object {
        val logger = KotlinLogging.logger {}

        fun download(url: URL, location: File): Modpack {
            Util.download(url, location)
            return Util.json.parse(serializer(), location.readText())
        }
    }

    fun process(root: File) {
        packages.forEach {p ->
            p.process(root)
            Thread.yield()
        }
    }
}