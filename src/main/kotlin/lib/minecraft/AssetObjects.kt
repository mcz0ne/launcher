package lib.minecraft

import kotlinx.serialization.Serializable
import lib.Util
import java.io.File
import java.net.URL

@Serializable
data class AssetObjects(
    val objects: HashMap<String, AssetObject>
) {
    companion object {
        fun download(url: URL, json: File, sha1: String): AssetObjects {
            Util.download(url, json, sha1)
            return parse(json)
        }

        fun parse(json: File): AssetObjects {
            return Util.json.parse(serializer(), json.readText())
        }
    }
}