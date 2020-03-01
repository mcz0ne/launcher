package lib.minecraft

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import lib.Util
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.security.MessageDigest
import java.util.jar.JarFile
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

private val logger = KotlinLogging.logger {}
private val json = Json(JsonConfiguration.Stable)
private val http = OkHttpClient()
private const val VersionManifestURL: String = "https://launchermeta.mojang.com/mc/game/version_manifest.json"

fun fetchVersions(): List<VersionManifestVersion> {
    logger.info("fetching available Minecraft versions...")
    val req = Request.Builder().url(VersionManifestURL).build()
    http.newCall(req).execute().use {
        if (!it.isSuccessful) {
            throw Exception("Failed to fetch version_manifest.json")
        }

        val res = json.parse(VersionManifest.serializer(), it.body!!.string())
        logger.debug("Found {} available versions", res.versions.count())

        return res.versions
    }
}

fun fetchVersion(url: URL): Version {
    logger.info("fetching version info from {}...", url)
    val req = Request.Builder().url(url).build()
    http.newCall(req).execute().use {
        if (!it.isSuccessful) {
            throw Exception("Failed to fetch version data")
        }

        return json.parse(Version.serializer(), it.body!!.string())
    }
}

fun fetchAssetIndex(url: URL): AssetObjects {
    logger.info("fetching asset index from {}...", url)
    val req = Request.Builder().url(url).build()
    http.newCall(req).execute().use {
        if (!it.isSuccessful) {
            throw Exception("Failed to fetch version data")
        }

        return json.parse(AssetObjects.serializer(), it.body!!.string())
    }
}

fun sha(file: File): String {
    val sha1 = MessageDigest.getInstance("SHA-1")
    BufferedInputStream(FileInputStream(file)).use {
        val buf = it.readBytes()
        sha1.update(buf)
        return HexBinaryAdapter().marshal(sha1.digest()).toLowerCase()
    }
}

fun download(url: URL, file: File, hash: String?) {
    if (file.exists() && hash != null) {
        logger.debug("{} found, comparing hashes", file)
        val fhash = sha(file)
        if (fhash == hash.toLowerCase()) {
            logger.info("{} already downloaded, skipping", url)
            return
        }
    }

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

        if (hash != null) {
            val fhash = sha(f)
            if (fhash != hash.toLowerCase()) {
                f.delete()
                throw Exception("different hash, expected ${hash.toLowerCase()} and got $fhash")
            }
        }

        file.parentFile.mkdirs()
        f.copyTo(file, true)
        f.delete()
    }
}

fun extract(jarFile: File, destDir: File, exclude: List<String> = listOf()) {
    logger.debug("Extracting {}", jarFile)
    val jar = JarFile(jarFile)

    val enumEntries = jar.entries()
    while (enumEntries.hasMoreElements()) {
        val file = enumEntries.nextElement()
        if (exclude.any { file.name.startsWith(it) }) {
            continue
        }

        val f = File(destDir.toString() + File.separator + file.name)
        if (file.isDirectory) {
            f.mkdir()
            continue
        }

        val s = f.outputStream()
        val cs = jar.getInputStream(file)
        cs.copyTo(s)
        cs.close()
        s.close()
    }
    jar.close()
}

fun install(version: String, location: File): Version {
    logger.info("installing minecraft v{}", version)
    val versionManifest = fetchVersions().filter { it.id == version }.sortedBy { it.time }[0]

    val versionInfo = fetchVersion(versionManifest.url)
    val client = versionInfo.downloads["client"] ?: error("client download not found")

    logger.info("Downloading minecraft.jar")
    download(client.url, File(location, "versions/$version/$version.jar"), client.sha1)
    File(location, "versions/$version/$version.json").writeText(json.stringify(Version.serializer(), versionInfo))

    val f = VersionArgumentFeature(isDemoUser = false, hasCustomResolution = true)
    val os = VersionArgumentOS(
        name = Util.OS.detect().toString().toLowerCase(),
        version = System.getProperty("os.version", "0"),
        arch = System.getProperty("os.arch", "32")
    )

    logger.info("Downloading {} libraries...", versionInfo.libraries.count())
    val libLocation = File(location, "libraries")
    versionInfo.libraries.forEach {
        if (!it.isAllowed(f, os)) {
            logger.debug("Skipping {}", it.name)
        } else {
            logger.debug("Downloading {}", it.name)
            download(
                it.downloads.artifact.url,
                File(libLocation, it.downloads.artifact.path!!),
                it.downloads.artifact.sha1
            )

            if (it.natives != null && it.natives.containsKey(os.name)) {
                val natives = it.downloads.classifiers!![it.natives[os.name]]!!
                download(natives.url, File(libLocation, natives.path!!), natives.sha1)
                if (it.extract != null) {
                    extract(File(libLocation, natives.path), File(libLocation, "natives"), it.extract.exclude)
                }
            }
        }
    }

    logger.info("Fetching asset index")
    val assets = fetchAssetIndex(versionInfo.assetIndex!!.url)
    val assetBase = URL("http://resources.download.minecraft.net/")
    val assetLocation = File(location, "assets/objects")
    logger.info("Downloading {} assets", assets.objects.count())
    assets.objects.forEach {
        val fname = "${it.value.hash.substring(0, 2)}/${it.value.hash}"
        download(URL(assetBase, fname), File(assetLocation, fname), it.value.hash)
    }
    File(location, "assets/index/${versionInfo.assets}.json").writeText(
        json.stringify(
            AssetObjects.serializer(),
            assets
        )
    )

    return versionInfo
}