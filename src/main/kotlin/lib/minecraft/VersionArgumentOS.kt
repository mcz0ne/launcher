package lib.minecraft

import kotlinx.serialization.Serializable
import lib.OS

@Serializable
data class VersionArgumentOS(
    val name: String? = null,
    val version: String? = null,
    val arch: String? = null
) {
    companion object {
        private lateinit var sysOS: VersionArgumentOS

        fun fromSystem(): VersionArgumentOS {
            if (!::sysOS.isInitialized) {
                sysOS = VersionArgumentOS(
                    name = OS.detect().toString().toLowerCase(),
                    version = System.getProperty("os.version", "0"),
                    arch = System.getProperty("os.arch", "unkown")
                )
            }

            return sysOS
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is VersionArgumentOS) {
            return super.equals(other)
        }

        if (name != null && name != other.name) {
            return false
        }

        if (version != null && version != other.version) {
            return false
        }

        if (arch != null && arch != other.arch) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "OS<name:${name ?: "null"},version:${version ?: "null"},arch:${arch ?: "null"}>"
    }
}