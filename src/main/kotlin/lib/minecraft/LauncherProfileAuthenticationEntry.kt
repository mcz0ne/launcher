package lib.minecraft

import kotlinx.serialization.Serializable
import lib.yggdrasil.Account

@Serializable
data class LauncherProfileAuthenticationEntry(
    val accessToken: String,
    val profiles: Map<String, LauncherProfileAuthenticationProfile>,
    val properties: List<String> = listOf(),
    val username: String
) {
    companion object {
        fun fromYggdrasil(acc: Account): Pair<String, LauncherProfileAuthenticationEntry> {
            return Pair(
                acc.id, LauncherProfileAuthenticationEntry(
                    accessToken = acc.accessToken,
                    username = acc.email,
                    profiles = mapOf(acc.uuid to LauncherProfileAuthenticationProfile(acc.username))
                )
            )
        }
    }

    fun toYggdrasil(id: String): Account {
        return Account(
            id = id,
            uuid = profiles.keys.first(),
            email = username,
            username = profiles.values.first().displayName,
            accessToken = accessToken
        )
    }
}