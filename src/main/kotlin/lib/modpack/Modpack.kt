package lib.modpack

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import br.com.gamemods.nbtmanipulator.NbtList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import lib.Util
import lib.minecraft.VersionArgumentFeature
import lib.minecraft.VersionArgumentOS
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URL
import java.security.MessageDigest
import java.util.jar.JarFile
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

@Serializable
data class File(
    val url: String,
    val sha1: String? = null,
    val homepage: String? = null,
    val target: String? = null,
    val extract: Boolean = false,
    val ignore: Boolean = false
) {
    private fun resolveTarget(): String {
        if (target != null) {
            return target
        }

        return when (java.io.File(URL(url).path).extension) {
            "jar" -> "mods"
            "toml", "cfg", "json", "properties" -> "config"
            else -> "."
        }
    }

    fun fileName(root: java.io.File, id: String): java.io.File {
        var tgt = java.io.File(root, resolveTarget())
        if (tgt.exists() && tgt.isDirectory) {
            tgt = java.io.File(tgt, "$id.${java.io.File(URL(url).path).extension}")
        }

        return tgt
    }

    fun download(root: java.io.File, id: String) {
        if (ignore) {
            logger.info("ignoring")
            return
        }

        val f = fileName(root, id)
        download(URL(url), f, sha1)

        if (extract) {
            extract(f)
        }
    }
}

@Serializable
data class Modpack(
    val minecraft: String,
    val forge: String? = null,
    val multiplayer: String? = null,
    val version: String = "1",
    val files: Map<String, File> = mapOf(),
    val remove: List<String> = listOf() // remove by id, set entry in files as ignore: true to resolve
) {
    fun processRemoveList(root: java.io.File) {
        logger.info("removing {} files", remove.count())
        remove.forEach {
            logger.info("removing {}", it)
            val file = files[it]
            if (file != null) {
                val f = file.fileName(root, it)
                if (f.exists()) {
                    f.delete()
                }
            }
        }
    }

    fun processDownloads(root: java.io.File) {
        logger.info("downloading {} files", files.count())
        files.forEach {
            logger.info("downloading {}", it.key)
            it.value.download(root, it.key)
        }
    }
}

private val logger = KotlinLogging.logger {}
private val json = Json(JsonConfiguration.Stable)
private val http = OkHttpClient()

fun sha(file: java.io.File): String {
    val sha1 = MessageDigest.getInstance("SHA-1")
    BufferedInputStream(FileInputStream(file)).use {
        val buf = it.readBytes()
        sha1.update(buf)
        return HexBinaryAdapter().marshal(sha1.digest()).toLowerCase()
    }
}

fun download(url: URL, file: java.io.File, hash: String?) {
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

        val f = java.io.File.createTempFile("download-", ".mcz")
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

fun extract(jarFile: java.io.File, exclude: List<String> = listOf()) {
    logger.debug("Extracting {}", jarFile)
    val jar = JarFile(jarFile)
    val destDir = jarFile.parentFile

    val enumEntries = jar.entries()
    while (enumEntries.hasMoreElements()) {
        val file = enumEntries.nextElement()
        if (exclude.any { file.name.startsWith(it) }) {
            continue
        }

        val f = java.io.File(destDir.toString() + java.io.File.separator + file.name)
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

fun install(name: String, url: URL, location: java.io.File, clean: Boolean = false): List<String> {
    val instanceLocation = java.io.File(location, "minecraft")
    if (clean) {
        logger.info("cleaning minecraft instance folder")
        instanceLocation.deleteRecursively()
        instanceLocation.mkdirs()
        listOf("config", "mods").forEach {
            java.io.File(instanceLocation, it).mkdir()
        }
    }

    logger.info("grabbing modpack {}", url)
    val req = Request.Builder().url(url).build()
    val res = http.newCall(req).execute()
    if (!res.isSuccessful) {
        throw Exception("Failed to fetch modpack info")
    }
    val modpack = json.parse(Modpack.serializer(), res.body!!.string())

    var mcVersion = lib.minecraft.install(modpack.minecraft, location)
    if (modpack.forge != null) {
        mcVersion = lib.forge.install(modpack.forge, location)
    }

    if (modpack.multiplayer != null) {
        val serversDat = java.io.File(instanceLocation, "servers.dat")
        val serversList: NbtList<NbtCompound> = if (serversDat.exists()) {
            (NbtIO.readNbtFile(
                serversDat,
                false
            ).tag as NbtList<*>).filterIsInstance<NbtCompound>() as NbtList<NbtCompound>
        } else {
            NbtList()
        }

        if (serversList.any { it.getNullableBooleanByte("managed") }) {
            serversList.find { it.getNullableBooleanByte("managed") }!!["ip"] = modpack.multiplayer
        } else {
            val c = NbtCompound()
            c["managed"] = true
            c["ip"] = modpack.multiplayer
            c["name"] = name
            serversList.add(c)
        }
        NbtIO.writeNbtFile(serversDat, NbtFile("servers", serversList), false)
    }

    // arg check
    val f = VersionArgumentFeature(isDemoUser = false, hasCustomResolution = true)
    val os = VersionArgumentOS(
        name = Util.OS.detect().toString().toLowerCase(),
        version = System.getProperty("os.version", "0"),
        arch = System.getProperty("os.arch", "32")
    )

    // build arg list
    val args: MutableList<String> = mutableListOf()
    mcVersion.arguments.jvm.forEach {
        args.addAll(it.arguments(f, os))
    }
    args.add(mcVersion.mainClass)
    mcVersion.arguments.game.forEach {
        args.addAll(it.arguments(f, os))
    }

    // replace some variables
    // missing variables:
    //   - ${auth_player_name}
    //   - ${auth_uuid}
    //   - ${auth_access_token}
    //   - ${resolution_width}
    //   - ${resolution_height}

    val libLoc = java.io.File(location, "libraries")

    return args.map {
        when (it) {
            "\${version_name}" -> modpack.minecraft
            "\${game_directory}" -> instanceLocation.toString()
            "\${assets_root}" -> java.io.File(location, "assets").toString()
            "\${assets_index_name}" -> mcVersion.assets
            "\${user_type}" -> "MOJANG"
            "\${version_type}" -> mcVersion.type.toString().toLowerCase()
            "\${classpath}" -> mcVersion.libraries
                .filter { l -> l.isAllowed(f, os) && !l.downloads.artifact.path.isNullOrBlank() }
                .joinToString(java.io.File.separator) { l ->
                    java.io.File(libLoc, l.downloads.artifact.path!!).toString()
                }
            else -> when {
                it.contains("\${natives_directory}") -> it.replace(
                    "\${natives_directory}",
                    java.io.File(libLoc, "natives").toString()
                )
                it.contains("\${launcher_name}") -> it.replace(
                    "\${launcher_name}",
                    name
                )
                it.contains("\${launcher_version}") -> it.replace(
                    "\${launcher_version}",
                    modpack.version
                )
                else -> it
            }
        }
    }
}
