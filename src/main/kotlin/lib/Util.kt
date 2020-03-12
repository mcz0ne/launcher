package lib

import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest
import java.util.jar.JarFile
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

object Util {
    private val logger = KotlinLogging.logger { }

    val json = kotlinx.serialization.json.Json(
        JsonConfiguration.Stable.copy(
            strictMode = false,
            prettyPrint = true
        )
    )

    val http = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun sha1(file: File): String {
        val sha1 = MessageDigest.getInstance("SHA-1")
        file.inputStream().buffered().use {
            val buf = it.readBytes()
            sha1.update(buf)
        }
        val hash = HexBinaryAdapter().marshal(sha1.digest()).toLowerCase()
        logger.trace("Generated hash {} for file {}", hash, file.name)
        return hash
    }

    fun download(url: URL, target: File, shaHash: String? = null): Boolean {
        logger.debug("Preparing {} for download", url)
        if (target.exists() && shaHash != null) {
            logger.trace("Target {} found, comparing hashes", target)
            val targetHash = sha1(target)
            if (targetHash == shaHash.toLowerCase()) {
                logger.debug("Hash match, skipping")
                return false
            }
        }

        if (!target.parentFile.exists() && !target.parentFile.mkdirs()) {
            throw IOException("Failed to create parent directory")
        }

        val f = File.createTempFile("download-", ".mcz")
        logger.trace("Saving stream to {}", f)
        f.outputStream().use { stream -> download(url).use { body -> body.copyTo(stream) } }

        if (shaHash != null) {
            logger.trace("Comparing hashes...")

            val fhash = sha1(f)
            if (fhash != shaHash.toLowerCase()) {
                f.delete()
                throw Exception("Different hash, expected ${shaHash.toLowerCase()} and got $fhash")
            }
        }

        logger.trace("Moving temporary file from {} to {}", f, target)
        f.copyTo(target, true)
        f.delete()
        return true
    }

    fun download(url: URL): InputStream {
        val req = Request.Builder().url(url).build()
        logger.debug("Downloading {}", url)
        val response = http.newCall(req).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to request $url")
        }

        return response.body!!.byteStream()
    }


    fun extract(zip: File, destDir: File, exclude: List<String> = listOf()) {
        logger.debug("Extracting {}", zip)
        destDir.mkdirs()
        JarFile(zip).use { jar ->
            val enumEntries = jar.entries()
            while (enumEntries.hasMoreElements()) {
                val file = enumEntries.nextElement()
                if (exclude.any { file.name.startsWith(it) || file.name.startsWith("/$it") }) {
                    continue
                }

                val f = File(destDir, file.name)
                if (file.isDirectory) {
                    f.mkdir()
                    continue
                }

                // just to make sure file exists
                f.parentFile.mkdirs()
                logger.trace(" - inflating {}: {} -> {}", file, file.compressedSize, file.size)
                f.outputStream().use { outStream ->
                    jar.getInputStream(file).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                // yield thread between files
                Thread.yield()
            }
            jar.close()
        }
    }
}