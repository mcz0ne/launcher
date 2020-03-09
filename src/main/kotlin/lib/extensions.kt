package lib

import java.io.File
import java.net.URL


fun String.file(): File {
    return File(this)
}

fun String.url(): URL {
    return URL(this)
}

fun File.join(vararg files: String): File {
    var joined: File = this
    files.forEach {
        joined = File(joined, it)
    }
    return joined
}
