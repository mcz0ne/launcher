import lib.OS

const val FORGE1_WRAPPER = "https://github.com/mcz0ne/forge1installer/releases/latest/download/forge1.jar"
const val FORGE2_WRAPPER = "https://github.com/mcz0ne/forge2installer/releases/latest/download/forge2.jar"

val JAVA_EXECUTABLE = if (OS.detect().isWin) "javaw.exe" else "java"