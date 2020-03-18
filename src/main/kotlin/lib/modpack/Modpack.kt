package lib.modpack

import kotlinx.serialization.Serializable
import lib.Util
import mu.KotlinLogging
import java.io.File
import java.net.URL

@Serializable
data class Modpack(
    val minecraft: String,
    val forge: String,
    val multiplayer: String? = null,

    val serversync: String,
    val serversyncport: Int = 38067,

    val minMem: String = "2G",
    val maxMem: String = "4G",
    val javaOpts: List<String> = listOf(
        "-XX:+UseG1GC",
        "-Dsun.rmi.dgc.server.gcInterval=2147483646",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:G1NewSizePercent=20",
        "-XX:G1ReservePercent=20",
        "-XX:MaxGCPauseMillis=50",
        "-XX:G1HeapRegionSize=32M"
    )
) {
    companion object {
        val logger = KotlinLogging.logger {}

        fun download(url: URL, location: File): Modpack {
            Util.download(url, location)
            return Util.json.parse(serializer(), location.readText())
        }
    }
}