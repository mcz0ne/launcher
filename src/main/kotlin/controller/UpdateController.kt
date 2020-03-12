package controller

import FORGE1_WRAPPER
import FORGE2_WRAPPER
import JAVA_EXECUTABLE
import app.LauncherApp
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import lib.Util
import lib.file
import lib.join
import lib.minecraft.*
import lib.modpack.Modpack
import lib.url
import lib.yggdrasil.Account
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import runtimeDirectories
import tornadofx.Controller
import tornadofx.EventBus
import tornadofx.FXEvent
import tornadofx.task
import view.MainView
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import kotlin.collections.component1
import kotlin.collections.component2

class UpdateController : Controller() {
    class UpdateAvailable : FXEvent(runOn = EventBus.RunOn.ApplicationThread)
    private enum class UpdateStep(val step: Long) {
        PREPARE(0),
        DOWNLOAD_MC(1),
        DOWNLOAD_MC_LIBS(2),
        DOWNLOAD_MC_ASSETS(3),
        DOWNLOAD_FORGE(4),
        DOWNLOAD_MODPACK(5),

        DONE(6);

        fun next(): UpdateStep {
            return values()[step.toInt() + 1]
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private const val f1WrapperFilename = "forge1.jar"
        private const val f2WrapperFilename = "forge2.jar"
        private const val forgeInstallLink =
            "https://files.minecraftforge.net/maven/net/minecraftforge/forge/%1\$s/forge-%1\$s-installer.jar"
    }

    private val mainView: MainView by inject()
    private val launcherApp = app as LauncherApp

    val updateAvailableProperty = SimpleBooleanProperty()
    var updateAvailable: Boolean
        get() = updateAvailableProperty.get()
        private set(v) {
            updateAvailableProperty.set(v)
        }

    val canLaunchProperty = SimpleBooleanProperty()
    var canLaunch: Boolean
        get() = canLaunchProperty.get()
        private set(v) {
            canLaunchProperty.set(v)
        }

    private val cacheDir: File = runtimeDirectories.cacheDir.file().join(launcherApp.launcherConfig.id)
    private val configDir: File = runtimeDirectories.configDir.file().join(launcherApp.launcherConfig.id)
    internal val dataDir: File = runtimeDirectories.dataDir.file().join(launcherApp.launcherConfig.id)

    private val features = VersionArgumentFeature(hasCustomResolution = true)
    private val os = VersionArgumentOS.fromSystem()

    var modpack: Modpack? = null
        @Synchronized private set

    var installedVersion: String?
        get() {
            synchronized(this) {
                return try {
                    dataDir.join("version").readText()
                } catch (ex: FileNotFoundException) {
                    null
                }
            }
        }
        private set(v) {
            synchronized(this) {
                val f = dataDir.join("version")
                if (v == null) {
                    f.delete()
                } else {
                    f.writeText(v)
                }
            }
        }

    init {
        logger.trace("creating cache, config and data dirs for {}", launcherApp.launcherConfig.id)
        cacheDir.mkdirs()
        configDir.mkdirs()
        dataDir.mkdirs()
    }

    fun check() {
        logger.debug("checking for modpack update")
        mainView.taskProgressView.tasks.add(task {
            updateTitle("Modpack Update Checker")
            updateMessage("downloading and parsing manifest...")
            modpack = Modpack.download(
                launcherApp.launcherConfig.url,
                runtimeDirectories.dataDir.file().join(launcherApp.launcherConfig.id, "modpack.json")
            )

            if (installedVersion != modpack!!.version) {
                fire(UpdateAvailable())
            } else {
                canLaunch = true
            }

            updateMessage("finished!")
            updateProgress(1, 1)
        })
    }

    fun play(acc: Account): List<String> {
        logger.debug("launching modpack [mc:{} | forge:{}]", modpack!!.minecraft, modpack!!.forge)

        // read launcher profile first
        val profiles = LauncherProfiles.parse(dataDir.join("launcher_profiles.json")).profiles
        val v = if (modpack!!.forge != null) {
            (profiles["forge"] ?: error("forge profile not found")).lastVersionId
        } else {
            modpack!!.minecraft
        }

        // read version file
        val profile = Minecraft.parse(runtimeDirectories.dataDir.file().join("versions", v, "$v.json"))
        logger.debug("building arguments")
        val args = profile.launchArgs(features, os)
        logger.trace("Source argument list:")
        args.forEach { logger.trace("> {}", it) }

        val libLoc = runtimeDirectories.dataDir.file().join("libraries")
        return args.map { arg ->
            when (arg) {
                "\${classpath}" -> profile.allLibraries(features, os)
                    .joinToString(File.pathSeparator) { libLoc.join(it).toString() } + File.pathSeparator +
                        runtimeDirectories.dataDir.file()
                            .join("versions", modpack!!.minecraft, "${modpack!!.minecraft}.jar")
                "\${main_class}" -> profile.mainClass
                "\${auth_player_name}" -> acc.username
                "\${version_name}" -> modpack!!.minecraft
                "\${game_directory}" -> dataDir.toString()
                "\${assets_root}" -> runtimeDirectories.dataDir.file().join("assets").toString()
                "\${assets_index_name}" -> if (profile.assets.isBlank()) profile.inherited!!.assets else profile.assets
                "\${auth_uuid}" -> acc.uuid
                "\${user_access_token}" -> acc.accessToken
                "\${auth_access_token}" -> acc.accessToken
                "\${user_type}" -> if (acc.email.contains("@")) "MOJANG" else "LEGACY"
                "\${version_type}" -> modpack!!.version
                "\${resolution_width}" -> launcherApp.instance.minecraftWidth.toString()
                "\${resolution_height}" -> launcherApp.instance.minecraftHeight.toString()
                else -> when {
                    arg.contains("\${natives_directory}") -> arg.replace(
                        "\${natives_directory}",
                        libLoc.join("natives").toString()
                    )
                    arg.contains("\${launcher_name}") -> arg.replace(
                        "\${launcher_name}",
                        launcherApp.launcherConfig.name
                    )
                    arg.contains("\${launcher_version}") -> arg.replace(
                        "\${launcher_version}",
                        modpack!!.version
                    )
                    else -> arg
                }
            }
        }
    }

    fun update() {
        mainView.taskProgressView.tasks.add(task(daemon = true) {
            var step = UpdateStep.PREPARE

            updateTitle("Minecraft Update")
            val mcVersion = modpack!!.minecraft

            lateinit var requestedVersion: VersionListEntry
            lateinit var manifest: Minecraft
            val versionDir = runtimeDirectories.dataDir.file().join("versions")

            while (step != UpdateStep.DONE) {
                if (isCancelled) {
                    return@task
                }

                when (step) {
                    UpdateStep.PREPARE -> {
                        updateMessage("fetching version list...")
                        val vl = Minecraft.versionList()
                        requestedVersion = vl.versions.find { it.id == modpack!!.minecraft }
                            ?: throw Exception("Requested Minecraft version ${modpack!!.minecraft} not found on Mojang servers")
                    }
                    UpdateStep.DOWNLOAD_MC -> {
                        updateMessage("verifying minecraft install...")
                        manifest = try {
                            Minecraft.parse(versionDir.join(mcVersion, "$mcVersion.json"))
                        } catch (ex: FileNotFoundException) {
                            Minecraft.download(requestedVersion.url!!, versionDir.join(mcVersion, "$mcVersion.json"))
                        }

                        val clientJarInfo =
                            manifest.downloads["client"] ?: throw Exception("Minecraft client jar info not found")
                        val clientJar = versionDir.join(mcVersion, "$mcVersion.jar")
                        if (!clientJar.exists() || Util.sha1(clientJar) != clientJarInfo.sha1.toLowerCase()) {
                            updateMessage("downloading minecraft client jar")
                            Util.download(clientJarInfo.url!!, clientJar, clientJarInfo.sha1)
                        }

                        if (!dataDir.join("launcher_profiles.json").exists()) {
                            logger.trace("creating launcher_profiles.json")
                            UpdateController::class.java.getResourceAsStream("/launcher_profiles.json").use { input ->
                                dataDir.join("launcher_profiles.json").outputStream()
                                    .use { output ->
                                        input.copyTo(output)
                                    }
                            }
                        }
                    }
                    UpdateStep.DOWNLOAD_MC_LIBS -> {
                        val libCount = manifest.libraries.count()
                        val libFolder = runtimeDirectories.dataDir.file().join("libraries")
                        val libNatives = libFolder.join("natives")

                        libNatives.mkdirs()

                        val msgTemplate = "downloading libraries... (%d / ${manifest.libraries.count()}): %s"
                        updateMessage(msgTemplate.format(0, "..."))

                        for (i in 0 until libCount) {
                            if (isCancelled) {
                                return@task
                            }

                            val lib = manifest.libraries[i]
                            updateMessage(msgTemplate.format(i + 1, lib.name))
                            if (!lib.isAllowed(features, os)) {
                                continue
                            }

                            if (lib.downloads.artifact != null) {
                                Util.download(
                                    lib.downloads.artifact.url!!,
                                    libFolder.join(lib.location),
                                    lib.downloads.artifact.sha1
                                )
                            }

                            if (lib.natives != null && lib.natives.containsKey(os.name)) {
                                val classifier = lib.natives[os.name]!!
                                val natives =
                                    lib.downloads.classifiers?.get(classifier) ?: error("natives library not found")
                                Util.download(natives.url!!, libFolder.join(natives.path!!), natives.sha1)
                                Util.extract(libFolder.join(natives.path), libNatives, lib.extract?.exclude ?: listOf())
                            }
                            Thread.yield()
                        }
                    }
                    UpdateStep.DOWNLOAD_MC_ASSETS -> {
                        updateMessage("Fetching asset index...")
                        val assetDir = runtimeDirectories.dataDir.file().join("assets")
                        val assetBaseURL = URL("http://resources.download.minecraft.net/")
                        val assetBaseDir = assetDir.join("objects")
                        val assetIndex = AssetObjects.download(
                            manifest.assetIndex!!.url!!,
                            assetDir.join("indexes", "${manifest.assets}.json"),
                            manifest.assetIndex!!.sha1
                        )

                        val msgTemplate = "downloading assets... (%d / ${assetIndex.objects.size}): %s"
                        updateMessage(msgTemplate.format(0, "..."))

                        val assets = assetIndex.objects.entries
                        for (i in 0 until assets.size) {
                            if (isCancelled) {
                                return@task
                            }

                            val (key, obj) = assets.elementAt(i)
                            updateMessage(msgTemplate.format(i + 1, key))
                            val fname = "${obj.hash.substring(0, 2)}/${obj.hash}"
                            Util.download(URL(assetBaseURL, fname), assetBaseDir.join(fname), obj.hash)
                            Thread.yield()
                        }
                    }
                    UpdateStep.DOWNLOAD_FORGE -> {
                        updateMessage("verifying forge install...")
                        val forgeVersion = modpack!!.forge
                        if (forgeVersion != null && !(dataDir.join("forgeVersion")
                                .exists() && dataDir.join("forgeVersion").readText() == forgeVersion)
                        ) {
                            updateMessage("Downloading forge installer wrapper...")

                            val globalProfiles = dataDir.parentFile.join("launcher_profiles.json")
                            logger.trace("deleting global profiles")
                            if (globalProfiles.exists()) {
                                globalProfiles.delete()
                            }

                            logger.trace("copying over local profile to global")
                            dataDir.join("launcher_profiles.json").copyTo(globalProfiles, true)

                            // check for old or new forge installer
                            val (major, minor) = manifest.id.split(".").map { it.toIntOrNull() ?: 0 }
                            if (major > 1 || minor > 12) {
                                logger.info("patching forge using installer v2")
                                val f2w = runtimeDirectories.cacheDir.file().join(f2WrapperFilename)
                                val f2wHash =
                                    Util.download("$FORGE2_WRAPPER.sha1sum".url())
                                        .use { it.bufferedReader().readText() }
                                        .split(" ").first()
                                if (!f2w.exists() || Util.sha1(f2w) != f2wHash.toLowerCase()) {
                                    Util.download(FORGE2_WRAPPER.url(), f2w, f2wHash)
                                }

                                updateMessage("downloading forge installer")
                                val forgeLink = forgeInstallLink.format(forgeVersion).url()
                                val f2 = f2w.parentFile.join(FilenameUtils.getName(forgeLink.path))
                                if (!f2.exists()) {
                                    Util.download(forgeLink, f2)
                                }

                                updateMessage("launching patcher")
                                val exitCode = ProcessBuilder(
                                    System.getProperty("java.home").file().join("bin", JAVA_EXECUTABLE).toString(),
                                    "-cp", listOf(f2.toString(), f2w.toString()).joinToString(File.pathSeparator),
                                    "moe.z0ne.mc.forge2installer.MainKt",
                                    runtimeDirectories.dataDir
                                )
                                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                                    .directory(runtimeDirectories.dataDir.file())
                                    .start()
                                    .waitFor()

                                logger.trace("Patcher finished: {}", exitCode)
                            } else {
                                logger.info("patching forge using installer v1")
                                val f1w = runtimeDirectories.cacheDir.file().join(f1WrapperFilename)
                                val f1wHash =
                                    Util.download("$FORGE1_WRAPPER.sha1sum".url())
                                        .use { it.bufferedReader().readText() }
                                        .split(" ").first()
                                if (!f1w.exists() || Util.sha1(f1w) != f1wHash.toLowerCase()) {
                                    Util.download(FORGE1_WRAPPER.url(), f1w, f1wHash)
                                }

                                updateMessage("downloading forge installer")
                                val forgeLink = forgeInstallLink.format(forgeVersion).url()
                                val f1 = f1w.parentFile.join(FilenameUtils.getName(forgeLink.path))
                                if (!f1.exists()) {
                                    Util.download(forgeLink, f1)
                                }

                                updateMessage("launching patcher")
                                val exitCode = ProcessBuilder(
                                    System.getProperty("java.home").file().join("bin", JAVA_EXECUTABLE).toString(),
                                    "-cp", listOf(f1.toString(), f1w.toString()).joinToString(File.pathSeparator),
                                    "moe.z0ne.mc.forge1installer.MainKt",
                                    runtimeDirectories.dataDir
                                )
                                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                                    .directory(runtimeDirectories.dataDir.file())
                                    .start()
                                    .waitFor()

                                logger.trace("Patcher finished: {}", exitCode)
                            }

                            logger.trace(
                                "copying {} to {}",
                                globalProfiles,
                                dataDir.join("launcher_profiles.json")
                            )
                            globalProfiles.copyTo(dataDir.join("launcher_profiles.json"), true)

                            logger.debug("checking for missing libraries...")
                            val forgeProfileVersion =
                                (LauncherProfiles.parse(dataDir.join("launcher_profiles.json")).profiles["forge"]
                                    ?: error("failed to get forge version")).lastVersionId
                            val forgeProfile = Minecraft.parse(
                                runtimeDirectories.dataDir.file()
                                    .join("versions", forgeProfileVersion, "$forgeProfileVersion.json")
                            )
                            val msgTemplate = "verifying libraries... (%d / %d): %s"
                            for (i in forgeProfile.libraries.indices) {
                                val lib = forgeProfile.libraries[i]
                                updateMessage(msgTemplate.format(i + 1, forgeProfile.libraries.size, lib.name))
                                val libFile = runtimeDirectories.dataDir.file().join("libraries", lib.location)
                                if (!libFile.exists()) {
                                    logger.warn("{} not found, force downloading!", lib.name)
                                    Util.download(
                                        lib.remoteLocation,
                                        libFile,
                                        lib.downloads.artifact?.sha1
                                    )
                                } else {
                                    logger.trace("{} OK", lib.name)
                                }
                                Thread.yield()
                            }

                            dataDir.join("forgeVersion").writeText(modpack!!.forge!!)
                        }
                    }
                    UpdateStep.DOWNLOAD_MODPACK -> {
                        modpack!!.process(dataDir)
                    }

                    UpdateStep.DONE -> throw Exception("Okay, Houston, we've had a problem here")
                }

                step = step.next()
                updateProgress(step.step, UpdateStep.DONE.step)
                Thread.yield()
            }

            logger.info("finished modpack update")
            installedVersion = modpack!!.version
            updateMessage("finished!")
            Platform.runLater { canLaunch = true }
            return@task
        })
    }
}