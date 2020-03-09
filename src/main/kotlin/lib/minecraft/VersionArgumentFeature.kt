package lib.minecraft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionArgumentFeature(
    @SerialName("is_demo_user")
    val isDemoUser: Boolean = false,
    @SerialName("has_custom_resolution")
    val hasCustomResolution: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is VersionArgumentFeature) {
            return super.equals(other)
        }

        if (isDemoUser && !other.isDemoUser) {
            return false
        }

        if (hasCustomResolution && !other.hasCustomResolution) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "Feature<isDemoUser:$isDemoUser,hasCustomResolution:$hasCustomResolution>"
    }
}