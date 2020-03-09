package lib.minecraft

import kotlinx.serialization.Serializable

@Serializable(with = VersionArgumentRuleActionSerializer::class)
enum class VersionArgumentRuleAction {
    ALLOW,
    DISALLOW
}