package controller

import DATA_DIR
import FORGE1_WRAPPER
import FORGE2_WRAPPER
import JAVA_EXECUTABLE
import LAUNCHER
import VERSION
import lib.Util
import lib.file
import lib.github.Release
import lib.join
import lib.minecraft.AssetObjects
import lib.minecraft.LauncherProfiles
import lib.minecraft.VersionArgumentFeature
import lib.minecraft.VersionArgumentOS
import lib.modpack.Modpack
import lib.url
import lib.yggdrasil.Account
import mu.KotlinLogging
import tornadofx.Controller
import tornadofx.TaskStatus
import tornadofx.task
import java.io.File
import java.net.URL
import lib.minecraft.Minecraft as MC

class Minecraft : Controller() {
    private val logger = KotlinLogging.logger { }
    lateinit var profile: Modpack

    fun install(workDir: File, ts: TaskStatus? = null, forceUpdateSync: Boolean = false) {
        task(daemon = true, taskStatus = ts) {
            logger.info("--- modpack")
            updateTitle("Installing Minecraft")
            updateMessage("grabbing server info...")
            profile = if (LAUNCHER.url.protocol == "file") {
                Util.json.parse(Modpack.serializer(), LAUNCHER.url.path.file().readText())
            } else {
                Util.download(LAUNCHER.url).use {
                    Util.json.parse(Modpack.serializer(), it.bufferedReader().use { br -> br.readText() })
                }
            }
            updateProgress(1, 4)

            logger.info("--- mc")
            updateMessage("checking minecraft install...")
            val mcManifestFile = workDir.join("version", profile.minecraft, "${profile.minecraft}.json")
            val mcJar = workDir.join("version", profile.minecraft, "${profile.minecraft}.jar")
            updateProgress(2, 4)

            if (!mcManifestFile.exists()) {
                updateMessage("downloading minecraft manifest...")
                val versionList = MC.versionList()
                val mcVersion = versionList.versions.find { it.id == profile.minecraft }!!

                mcManifestFile.parentFile.mkdirs()
                Util.download(
                    mcVersion.url!!,
                    mcManifestFile
                )
            }
            updateProgress(3, 4)

            val mcManifest = MC.parse(mcManifestFile)

            if (!mcJar.exists()) {
                updateMessage("downloading minecraft jar...")
                val client = mcManifest.downloads.getValue("client")
                Util.download(client.url!!, mcJar, client.sha1)
            }
            updateProgress(4, 4)

            val features = VersionArgumentFeature()
            val os = VersionArgumentOS.fromSystem()

            logger.info("--- libs")
            updateTitle("Downloading Libraries")
            val libCount = mcManifest.libraries.size.toLong()
            val libFolder = workDir.join("libraries")
            val libNatives = libFolder.join("natives")
            libNatives.mkdirs()

            mcManifest.libraries.forEachIndexed { i, lib ->
                updateMessage("${lib.name}...")
                updateProgress(i.toLong(), libCount)
                if (!lib.isAllowed(features, os)) {
                    return@forEachIndexed
                }

                if (lib.downloads.artifact != null) {
                    Util.download(
                        lib.remoteLocation,
                        libFolder.join(lib.location),
                        lib.downloads.artifact.sha1
                    )
                }

                if (lib.natives != null && lib.natives.containsKey(os.name)) {
                    val classifier = lib.natives.getValue(os.name!!)
                    if (lib.downloads.classifiers != null && lib.downloads.classifiers.containsKey(classifier)) {
                        val natives = lib.downloads.classifiers.getValue(classifier)
                        Util.download(natives.url!!, libFolder.join(natives.path!!), natives.sha1)
                        Util.extract(libFolder.join(natives.path), libNatives, lib.extract?.exclude ?: listOf())
                    }
                }

                Thread.yield()
            }
            updateProgress(libCount, libCount)

            logger.info("--- assets")
            updateTitle("Downloading Assets")
            updateMessage("Fetching asset index...")
            val assetDir = DATA_DIR.join("assets")
            val assetBaseURL = URL("http://resources.download.minecraft.net/")
            val assetBaseDir = assetDir.join("objects")
            val assetIndex = AssetObjects.download(
                mcManifest.assetIndex!!.url!!,
                assetDir.join("indexes", "${mcManifest.assets}.json"),
                mcManifest.assetIndex.sha1
            )
            var assetsDownloaded = mcManifest.assetIndex.size.toLong()

            assetIndex.objects.forEach { (key, obj) ->
                updateMessage("${key}...")
                updateProgress(assetsDownloaded, mcManifest.assetIndex.totalSize.toLong())

                val fname = "${obj.hash.substring(0, 2)}/${obj.hash}"
                Util.download(URL(assetBaseURL, fname), assetBaseDir.join(fname), obj.hash)

                assetsDownloaded += obj.size
            }

            logger.info("--- forge")
            updateTitle("Installing Forge")
            updateProgress(0, 3)
            updateMessage("verifying forge install...")
            val profiles = LauncherProfiles.parse(workDir.join(LauncherProfiles.FILENAME))
            if (!profiles.profiles.containsKey("forge") || !profiles.profiles.getValue("forge").lastVersionId.endsWith(
                    profile.forge.split("-").last()
                )
            ) {
                updateMessage("downloading wrapper...")
                val mcz = workDir.join(".mcz")
                val wrapperJar = mcz.join("wrapper.jar")
                val installerJar = mcz.join("forge.jar")
                mcz.mkdirs()

                val (major, minor) = mcManifest.id.split(".").map { it.toIntOrNull() ?: 0 }
                val wrapperVersion = if (major > 1 || minor > 12) {
                    val f2wHash =
                        Util.download("$FORGE2_WRAPPER.sha1sum".url())
                            .use { it.bufferedReader().readText() }
                            .split(" ").first()
                    Util.download(FORGE2_WRAPPER.url(), wrapperJar, f2wHash)
                    "2"
                } else {
                    val f1wHash =
                        Util.download("$FORGE1_WRAPPER.sha1sum".url())
                            .use { it.bufferedReader().readText() }
                            .split(" ").first()
                    Util.download(FORGE1_WRAPPER.url(), wrapperJar, f1wHash)
                    "1"
                }
                updateProgress(1, 3)

                updateMessage("downloading forge installer")
                val forgeLink =
                    URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/${profile.forge}/forge-${profile.forge}-installer.jar")
                Util.download(forgeLink, installerJar)
                updateProgress(2, 3)

                updateMessage("launching patcher")
                ProcessBuilder(
                    System.getProperty("java.home").file().join("bin", JAVA_EXECUTABLE).toString(),
                    "-cp", listOf(wrapperJar.toString(), installerJar.toString()).joinToString(File.pathSeparator),
                    "moe.z0ne.mc.forge${wrapperVersion}installer.MainKt",
                    workDir.toString()
                )
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .directory(workDir)
                    .start()
                    .waitFor()
                updateProgress(3, 3)
            }

            logger.info("--- forge libs")
            updateTitle("Downloading Forge Libraries")
            val forgeVersion =
                LauncherProfiles.parse(workDir.join(LauncherProfiles.FILENAME)).profiles.getValue("forge").lastVersionId
            val forgeProfile = MC.parse(
                workDir
                    .join("versions", forgeVersion, "${forgeVersion}.json")
            )
            val forgeLibCount = forgeProfile.libraries.size.toLong()
            forgeProfile.libraries.forEachIndexed { i, lib ->
                updateMessage("${lib.name}...")
                updateProgress(i.toLong(), forgeLibCount)
                if (!lib.isAllowed(features, os)) {
                    return@forEachIndexed
                }

                Util.download(
                    lib.remoteLocation,
                    libFolder.join(lib.location),
                    lib.downloads.artifact?.sha1
                )

                Thread.yield()
            }
            updateProgress(forgeLibCount, forgeLibCount)

            logger.info("--- serversync")
            updateTitle("Installing ServerSync")
            updateProgress(0, 3)
            updateMessage("verifying serversync install...")
            val ss = workDir.join("serversync.jar")
            if (forceUpdateSync || !ss.exists()) {
                updateMessage("fetching github release info")
                val release =
                    Util.download(URL("https://api.github.com/repos/superzanti/ServerSync/releases/latest")).use {
                        Util.json.parse(Release.serializer(), it.bufferedReader().use { br -> br.readText() })
                    }
                val dl = release.assets.find { a -> a.name.endsWith(".jar") }!!
                updateProgress(1, 3)

                updateMessage("downloading jar")
                Util.download(URL(dl.browserDownloadURL), ss)
            }
            updateProgress(2, 3)

            updateMessage("creating serversync config")
            val config = this.javaClass.getResourceAsStream("/serversync-client.cfg").use {
                it.bufferedReader().use { br -> br.readText() }
            }
            val ssDir = workDir.join("config", "serversync")
            ssDir.mkdirs()
            ssDir.join("serversync-client.cfg").writeText(
                config.format(profile.serversync, profile.serversyncport)
            )
            logger.info("--- done")
        }
    }

    fun launchArgs(workDir: File, acc: Account): List<String> {
        // read launcher profile first
        val v = LauncherProfiles.parse(workDir.join(LauncherProfiles.FILENAME)).profiles.getValue("forge").lastVersionId

        // read version file
        val features = VersionArgumentFeature()
        val os = VersionArgumentOS.fromSystem()

        val mcManifest = MC.parse(workDir.join("versions", v, "$v.json"))
        val args = mcManifest.launchArgs(features, os)

        val libLoc = workDir.join("libraries")
        return args.map { arg ->
            when (arg) {
                "\${classpath}" -> mcManifest.allLibraries(features, os)
                    .joinToString(File.pathSeparator) { libLoc.join(it).toString() } + File.pathSeparator +
                        DATA_DIR
                            .join("versions", profile.minecraft, "${profile.minecraft}.jar")
                "\${main_class}" -> mcManifest.mainClass
                "\${auth_player_name}" -> acc.username
                "\${version_name}" -> profile.minecraft
                "\${game_directory}" -> workDir.toString()
                "\${assets_root}" -> workDir.join("assets").toString()
                "\${assets_index_name}" -> if (mcManifest.assets.isBlank()) mcManifest.inherited!!.assets else mcManifest.assets
                "\${auth_uuid}" -> acc.uuid
                "\${user_access_token}" -> acc.accessToken
                "\${auth_access_token}" -> acc.accessToken
                "\${user_type}" -> if (acc.email.contains("@")) "MOJANG" else "LEGACY"
                "\${version_type}" -> mcManifest.type.toString()
                else -> when {
                    arg.contains("\${natives_directory}") -> arg.replace(
                        "\${natives_directory}",
                        libLoc.join("natives").toString()
                    )
                    arg.contains("\${launcher_name}") -> arg.replace(
                        "\${launcher_name}",
                        LAUNCHER.getProperty("name", LAUNCHER.getProperty("id", "mcz"))
                    )
                    arg.contains("\${launcher_version}") -> arg.replace(
                        "\${launcher_version}",
                        VERSION
                    )
                    else -> arg
                }
            }
        }
    }
}
