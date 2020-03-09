package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class VersionArgumentRule(
    val action: VersionArgumentRuleAction,
    val features: VersionArgumentFeature? = null,
    val os: VersionArgumentOS? = null
) {
    fun allowed(features: VersionArgumentFeature, os: VersionArgumentOS): Boolean {
        val result = action == VersionArgumentRuleAction.ALLOW

        val featuresOK = if (this.features != null) {
            this.features == features
        } else {
            true
        }

        val osOK = if (this.os != null) {
            this.os == os
        } else {
            true
        }

        return if (featuresOK && osOK) {
            result
        } else {
            !result
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (features != null) {
            sb.append(features.toString())
        }
        if (os != null) {
            sb.append(os.toString())
        }
        return sb.toString()
    }
}