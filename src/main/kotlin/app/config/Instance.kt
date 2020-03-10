package app.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import lib.Util
import tornadofx.observable
import java.io.File

@Serializable
data class Instance(
    var alwaysUpdate: Boolean = false,

    var minecraftWidth: Int = 1280,
    var minecraftHeight: Int = 720,

    var javaHome: String = System.getProperty("java.home"),
    var javaMinMem: String = "1G",
    var javaMaxMem: String = "4G",
    var javaOptions: String = listOf(
        "-XX:+UseG1GC", // G1GC tries to keep garbage collection predictable, so it never takes a long time (big lag spikes) and doesn't repeatedly take lots of short times (microstuttering)
        "-Dsun.rmi.dgc.server.gcInterval=2147483646", // This tells the RMI layer not to do a full GC every minute.
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:G1NewSizePercent=20", // put aside 20% of the heap as "new" space. Have free space available for object creation
        "-XX:G1ReservePercent=20",
        "-XX:MaxGCPauseMillis=50", // GC should take 50ms max (1 server tick)
        "-XX:G1HeapRegionSize=32M" // Work in 32MB steps to prevent slow collection for large objects
    ).joinToString(" ")
) {
    @Transient
    val minecraftWidthProperty = this.observable<Int>("minecraftWidth")

    @Transient
    val minecraftHeightProperty = this.observable<Int>("minecraftHeight")

    @Transient
    val javaMinMemProperty = this.observable<String>("javaMinMem")

    @Transient
    val javaMaxMemProperty = this.observable<String>("javaMaxMem")

    @Transient
    val javaHomeProperty = this.observable<String>("javaHome")

    @Transient
    val javaOptionsProperty = this.observable<String>("javaOptions")

    @Transient
    val alwaysUpdateProperty = this.observable<Boolean>("alwaysUpdate")

    companion object {
        fun load(file: File): Instance {
            val inst = if (!file.exists()) {
                file.parentFile.mkdirs()
                Instance()
            } else {
                Util.json.parse(serializer(), file.readText())
            }

            inst.location = file
            return inst
        }
    }

    @Transient
    var location: File? = null

    fun save() {
        location!!.writeText(Util.json.stringify(serializer(), this))
    }
}