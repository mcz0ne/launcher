package lib.forge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import lib.minecraft.Version
import lib.minecraft.VersionArguments
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLClassLoader
import java.util.function.Predicate
import kotlin.math.max

private val logger = KotlinLogging.logger {}
private val json = Json(JsonConfiguration.Stable)
private val http = OkHttpClient()
private const val forgeInstaller =
    "https://files.minecraftforge.net/maven/net/minecraftforge/forge/%1\$s/forge-%1\$s-installer.jar"

private val resources = object {}

fun downloadForge(version: String, file: File): Boolean {
    if (file.exists()) {
        logger.debug("{} found, skipping", file)
        return false
    }

    val url = forgeInstaller.format(version)
    val req = Request.Builder().url(url).build()
    logger.debug("Downloading {} to {}", url, file)
    http.newCall(req).execute().use {
        if (!it.isSuccessful) {
            throw Exception("failed to download file")
        }

        val f = File.createTempFile("download-", ".mcz")
        val s = f.outputStream()
        it.body!!.byteStream().copyTo(s)
        s.close()

        file.parentFile.mkdirs()
        f.copyTo(file, true)
        f.delete()

        return true
    }
}

fun loadJar(jar: File): ClassLoader {
    return URLClassLoader(
        arrayOf(jar.toURI().toURL()),
        ClassLoader.getSystemClassLoader()
    )
}

fun install(version: String, location: File, forceInstall: Boolean = false): Version {
    logger.info("installing minecraft forge v{}", version)
    val installer = File(location, "forge_installer-${version}.jar")
    if (downloadForge(version, File(location, "forge_installer-${version}.jar")) || forceInstall) {
        val lp = File(location, "launcher_profiles.json")
        if (!lp.exists()) {
            val rs = resources::class.java.getResourceAsStream("/launcher_profiles.json")
            val os = lp.outputStream()
            rs.copyTo(os)
            os.close()
            rs.close()
        }

        val cl = loadJar(installer)
        val forgeUtil = Class.forName("net.minecraftforge.installer.json.Util", false, cl)
        val forgeUtilLoadInstallProfile = forgeUtil.getDeclaredMethod("loadInstallProfile")
        val installProfile = forgeUtilLoadInstallProfile.invoke(forgeUtil)
        logger.debug("found install profile: {}", installProfile)

        val forgeProgressCallback = Class.forName("net.minecraftforge.installer.actions.ProgressCallback", false, cl)
        val progressCallback = forgeProgressCallback.getField("TO_STD_OUT").get(forgeProgressCallback)
        logger.debug("created progressCallback: {}", progressCallback)

        val forgeClientInstall = Class.forName("net.minecraftforge.installer.actions.ClientInstall", true, cl)
        val forgeClientInstallConstructor = forgeClientInstall.declaredConstructors[0] // has only one constructor
        val installAction = forgeClientInstallConstructor.newInstance(installProfile, progressCallback)
        logger.debug("created installAction: {}", installAction)

        val forgeClientInstallRun = forgeClientInstall.declaredMethods.find { it.name == "run" }!!
        logger.info("invoking ClientInstall.run")
        forgeClientInstallRun.invoke(installAction, location, Predicate<String> { true })
    }

    val forgeVersion = version.replace("-", "-forge-")
    val versionInfoFileNameTemplate = "versions/%1\$s/%1\$s.json"

    val versionInfoFile = File(location, versionInfoFileNameTemplate.format(forgeVersion))
    if (!versionInfoFile.exists()) {
        throw RuntimeException("forge version file not found")
    }
    val fv = json.parse(Version.serializer(), versionInfoFile.readText())

    val mcVersionFile = File(location, versionInfoFileNameTemplate.format(fv.inheritsFrom!!))
    val mcv = json.parse(Version.serializer(), mcVersionFile.readText())

    return Version(
        arguments = VersionArguments(
            game = mcv.arguments.game.toMutableList().plus(fv.arguments.game),
            jvm = mcv.arguments.jvm.toMutableList().plus(fv.arguments.jvm)
        ),

        assetIndex = mcv.assetIndex,
        assets = mcv.assets,
        id = fv.id,
        libraries = fv.libraries.plus(mcv.libraries),
        mainClass = fv.mainClass,
        minimumLauncherVersion = max(mcv.minimumLauncherVersion, fv.minimumLauncherVersion),
        time = fv.time,
        releaseTime = fv.releaseTime,
        type = fv.type
    )
}
