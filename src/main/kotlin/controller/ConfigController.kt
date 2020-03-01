package controller

import javafx.collections.ObservableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import lib.Yggdrasil
import mu.KotlinLogging
import tornadofx.*
import java.io.File
import java.util.*

class ConfigController : Controller() {
    @Serializable
    data class MinecraftConfiguration(
        var width: Int = 1280,
        var height: Int = 720,
        var fullscreen: Boolean = false
    )

    @Serializable
    data class JavaConfiguration(
        var minMem: String = "4G",
        var maxMem: String = "8G",
        var javaHome: String = System.getProperty("java.home"),
        var jvmOptions: String = listOf(
            "-XX:+UseG1GC", // G1GC tries to keep garbage collection predictable, so it never takes a long time (big lag spikes) and doesn't repeatedly take lots of short times (microstuttering)
            "-Dsun.rmi.dgc.server.gcInterval=2147483646", // This tells the RMI layer not to do a full GC every minute.
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:G1NewSizePercent=20", // put aside 20% of the heap as "new" space. Have free space available for object creation
            "-XX:G1ReservePercent=20",
            "-XX:MaxGCPauseMillis=50", // GC should take 50ms max (1 server tick)
            "-XX:G1HeapRegionSize=32M" // Work in 32MB steps to prevent slow collection for large objects
        ).joinToString(" ")
    )

    @Serializable
    data class Configuration(
        var clientToken: String = UUID.randomUUID().toString(),

        val accounts: MutableList<Yggdrasil.Account> = mutableListOf(),
        var selectedAccount: String = "",

        val minecraft: MinecraftConfiguration = MinecraftConfiguration(),
        val java: JavaConfiguration = JavaConfiguration()
    )

    class ConfigurationProperties(private val c: Configuration) {
        // minecraft
        var width: Int by property(c.minecraft.width)

        fun widthProperty() = getProperty(ConfigurationProperties::width)

        var height: Int by property(c.minecraft.height)
        fun heightProperty() = getProperty(ConfigurationProperties::height)

        var fullscreen: Boolean by property(c.minecraft.fullscreen)
        fun fullscreenProperty() = getProperty(ConfigurationProperties::fullscreen)

        // java
        var minMem: String by property(c.java.minMem)

        fun minMemProperty() = getProperty(ConfigurationProperties::minMem)

        var maxMem: String by property(c.java.maxMem)
        fun maxMemProperty() = getProperty(ConfigurationProperties::maxMem)

        var javaHome: String by property(c.java.javaHome)
        fun javaHomeProperty() = getProperty(ConfigurationProperties::javaHome)

        var jvmOptions: String by property(c.java.jvmOptions)
        fun jvmOptionsProperty() = getProperty(ConfigurationProperties::jvmOptions)

        fun update() {
            c.minecraft.width = width
            c.minecraft.height = height
            c.minecraft.fullscreen = fullscreen

            c.java.minMem = minMem
            c.java.maxMem = maxMem
            c.java.javaHome = javaHome
            c.java.jvmOptions = jvmOptions
        }
    }

    class ConfigurationModel(cp: ConfigurationProperties) : ItemViewModel<ConfigurationProperties>(cp) {
        val width = bind { item?.widthProperty() }
        val height = bind { item?.heightProperty() }
        val fullscreen = bind { item?.fullscreenProperty() }
        val minMem = bind { item?.minMemProperty() }
        val maxMem = bind { item?.maxMemProperty() }
        val javaHome = bind { item?.javaHomeProperty() }
        val jvmOptions = bind { item?.jvmOptionsProperty() }
    }

    companion object {
        private val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))
    }

    private val launcherConfigController: LauncherConfigController by inject()
    val appConfig: Configuration
    private val logger = KotlinLogging.logger {}
    private val ygg: Yggdrasil
    private val appConfigProperties: ConfigurationProperties

    private val accountsObserver: ObservableList<Yggdrasil.Account>

    init {
        appConfig = try {
            val f = File(launcherConfigController.folder(), "config.json")
            val t = f.inputStream().bufferedReader().readText()
            json.parse(Configuration.serializer(), t)
        } catch (e: Exception) {
            Configuration()
        }

        ygg = Yggdrasil(appConfig.clientToken)
        accountsObserver = appConfig.accounts.asObservable()
        appConfigProperties = ConfigurationProperties(appConfig)
        save()
    }

    fun save() {
        logger.debug("ensuring config path <{}>", launcherConfigController.folder())
        launcherConfigController.folder().mkdirs()

        logger.debug("saving configuration")
        val s = json.stringify(Configuration.serializer(), appConfig)
        File(launcherConfigController.folder(), "config.json").writeText(s)
    }

    fun verifyAccount(username: String): Boolean {
        val acc = accountsObserver.find { it.email == username }
        var ok = false
        try {
            if (acc != null) {
                if (!ygg.validate(acc.accessToken)) {
                    // no need to call logout
                    accountsObserver.remove(acc)

                    val newAcc = ygg.refresh(acc)
                    accountsObserver.add(newAcc)
                    ok = true
                } else {
                    ok = true
                }
            }
        } finally {
            save()
            return ok
        }
    }

    fun addAccount(username: String, password: String) {
        if (verifyAccount(username)) {
            return
        }

        accountsObserver.add(ygg.authenticate(username, password))
        if (accountsObserver.count() == 1) {
            appConfig.selectedAccount = accountsObserver[0].id
        }

        save()
    }

    fun removeAccount(id: String) {
        val acc =
            accountsObserver.find { it.email == id || it.username == id || it.accessToken == id }

        if (acc != null) {
            ygg.invalidate(acc.accessToken)
            accountsObserver.remove(acc)

            if (selectedAccount == acc) {
                selectedAccount = if (accountsObserver.count() > 1) {
                    accountsObserver[0]
                } else {
                    null
                }
            }

            save()
        }
    }

    var clientToken: String
        get() = appConfig.clientToken
        set(v) {
            appConfig.clientToken = v
            save()
        }

    var selectedAccount: Yggdrasil.Account?
        get() = accountsObserver.find { it.id == appConfig.selectedAccount }
        set(v) {
            if (v == null) {
                appConfig.selectedAccount = ""
            } else if (!accountsObserver.contains(v)) {
                return
            } else {
                appConfig.selectedAccount = v.id
            }
            save()
        }

    val accounts: ObservableList<Yggdrasil.Account>
        get() = accountsObserver

    val options: ConfigurationProperties
        get() = ConfigurationProperties(appConfig)
}