package lib

import java.net.URL
import java.util.*

class LauncherProperties : Properties() {
    val id: String
        get() = getProperty("id", "mcz")
    val name: String
        get() = getProperty("name", id)
    val news: URL
        get() = URL(getProperty("news", "https://minecraft.net"))
    val url: URL
        get() = URL(getProperty("url"))

    override fun toString(): String {
        return "LauncherProperties(id=${id};name=${name};url=${url})"
    }
}