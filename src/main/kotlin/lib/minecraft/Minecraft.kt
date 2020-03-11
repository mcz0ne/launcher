package lib.minecraft

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import lib.Util
import lib.join
import lib.url
import mu.KotlinLogging
import java.io.File
import java.net.URL

/*fun fetchAssetIndex(url: URL): AssetObjects {
    logger.info("fetching asset index from {}...", url)
    val req = Request.Builder().url(url).build()
    http.newCall(req).execute().use {
        if (!it.isSuccessful) {
            throw Exception("Failed to fetch version data")
        }

        return json.parse(AssetObjects.serializer(), it.body!!.string())
    }
}*/

@Serializable
data class Minecraft(
    val inheritsFrom: String? = null,
    val arguments: VersionArguments? = null,
    val assetIndex: VersionAssetIndex? = null,
    val assets: String = "",
    val downloads: Map<String, VersionDownload> = mapOf(),
    val id: String,
    val libraries: List<VersionLibrary>,
    val mainClass: String,
    val minimumLauncherVersion: Int = -1,
    val type: MinecraftVersionType,

    // 1.12 support
    val minecraftArguments: String? = null
) {
    companion object {
        internal val logger = KotlinLogging.logger {}
        private val VersionManifestURL: URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json".url()

        fun versionList(): VersionList {
            logger.info("fetching available Minecraft versions...")
            val buffer = Util.download(VersionManifestURL).use {
                it.bufferedReader().readText()
            }

            return Util.json.parse(VersionList.serializer(), buffer)
        }

        fun download(url: URL, json: File): Minecraft {
            Util.download(url, json)
            return parse(json)
        }

        fun parse(json: File): Minecraft {
            val mc = Util.json.parse(serializer(), json.readText())
            mc.location = json
            return mc
        }
    }

    val inherited: Minecraft?
        get() = if (inheritsFrom != null) {
            parse(location.parentFile.parentFile.join(inheritsFrom, "$inheritsFrom.json"))
        } else {
            null
        }

    @Transient
    lateinit var location: File

    fun allLibraries(feature: VersionArgumentFeature, os: VersionArgumentOS): List<String> {
        return libraries.filter { lib -> lib.isAllowed(feature, os) }
            .map { lib ->
                val (domain, pkg, ver) = lib.name.split(":")
                var path = "${domain.replace('.', '/')}/$pkg/$ver/$pkg-$ver.jar"
                if ((lib.downloads.artifact?.path) != null) {
                    path = lib.downloads.artifact.path
                }

                return@map path
            } + (inherited?.allLibraries(feature, os) ?: listOf())
    }

    fun launchArgs(feature: VersionArgumentFeature, os: VersionArgumentOS): List<String> {
        logger.debug("processing {} args", id)
        logger.trace("is modern? {}", arguments != null)
        logger.trace("is legacy? {}", minecraftArguments != null)
        logger.trace("mainClass: {}", mainClass)
        return if (minecraftArguments != null) {
            // set generic jvm args (based on 1.15.2 jvm args)
            listOf(
                "-Djava.library.path=\${natives_directory}", // path to natives
                "-Dminecraft.launcher.brand=\${launcher_name}",
                "-Dminecraft.launcer.version=\${launcher_version}",
                "-cp", "\${classpath}", // jar class path
                "\${main_class}"
            ) + if (feature.hasCustomResolution) {
                listOf(
                    "--width", "\${resolution_width}",
                    "--height", "\${resolution_height}"
                )
            } else {
                listOf()
            } + minecraftArguments.split(" ") // minecraft < 1.13 used minecraftArguments key to specify the launch args
        } else {
            (inherited?.launchArgs(feature, os) ?: listOf()) +
                    if (arguments == null) {
                        listOf()
                    } else {
                        val args = mutableListOf<String>()
                        arguments.jvm.forEach { argumentList ->
                            args.addAll(argumentList.arguments(feature, os))
                        }
                        if (arguments.jvm.isNotEmpty()) {
                            args.add("\${main_class}")
                        }
                        arguments.game.forEach { argumentList ->
                            args.addAll(argumentList.arguments(feature, os))
                        }
                        args.toList()
                    }
        }
    }
}