package lib.modpack

import kotlinx.serialization.Serializable
import lib.Util
import lib.join
import lib.minecraft.URLSerializer
import lib.modpack.Modpack.Companion.logger
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.URL

@Serializable
data class Package(
    val id: String,
    @Serializable(with = URLSerializer::class)
    val url: URL? = null,
    val sha1: String? = null,
    val homepage: String? = null,
    val target: String? = null,
    val action: Action = Action.EXTRACT
) {
    fun process(root: File) {
        when (action) {
            Action.EXTRACT -> {
                val f = root.join(target(), "$id.${extension()}")
                Util.download(url!!, f, sha1)
                Util.extract(f, root.join(target()))
            }
            Action.REMOVE -> root.join(target(), "$id.${extension()}").delete()
            Action.IGNORE -> {}
            Action.DOWNLOAD -> Util.download(url!!, root.join(target(), "$id.${extension()}"), sha1)
        }
    }

    private fun target(): String {
        if (target != null) {
            return target
        }

        if (url == null) {
            return "."
        }

        return when (FilenameUtils.getExtension(url.path)) {
            "jar" -> "mods"
            "toml", "cfg", "json", "properties" -> "config"
            else -> "."
        }
    }

    private fun extension(): String {
        return FilenameUtils.getExtension(url?.path ?: ".")
    }
}