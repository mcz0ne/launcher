import app.LauncherApp
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import io.github.soc.directories.ProjectDirectories
import lib.FXAppender
import lib.OS
import lib.file
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tornadofx.launch
import java.io.File

internal var runtimeDirectories: ProjectDirectories = ProjectDirectories.from("moe.z0ne.mc", "", "MCZ Launcher")

fun newPattern(lc: LoggerContext, colored: Boolean = false): PatternLayoutEncoder {
    val pattern = PatternLayoutEncoder()
    pattern.context = lc
    pattern.pattern = if (colored) {
        "%highlight(%.-1p) %gray([%d{HH:mm:ss.SSS}]) %cyan(\\(%t\\)) %magenta(%c{20}): %m%n"
    } else {
        "%.-1p [%d{HH:mm:ss.SSS}] \\(%t\\) %c{20}: %m%n"
    }
    pattern.start()

    return pattern
}

fun configureLogger(): Logger {
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext

    val consoleAppender = ConsoleAppender<ILoggingEvent>()
    consoleAppender.name = "console"
    consoleAppender.isWithJansi = !OS.detect().isWin
    consoleAppender.target = "System.out"
    consoleAppender.encoder = newPattern(lc, true)
    consoleAppender.context = lc
    consoleAppender.start()

    val fxAppender = FXAppender<ILoggingEvent>()
    fxAppender.name = "fx"
    fxAppender.encoder = newPattern(lc)
    fxAppender.context = lc
    fxAppender.start()

    val fileAppender = FileAppender<ILoggingEvent>()
    fileAppender.name = "file"
    fileAppender.encoder = newPattern(lc)
    fileAppender.isAppend = false
    fileAppender.file = File(runtimeDirectories.cacheDir.file(), "app.log").path
    fileAppender.context = lc
    fileAppender.start()

    val log = lc.getLogger("root")
    log.isAdditive = false
    log.detachAndStopAllAppenders()
    log.addAppender(consoleAppender)
    log.addAppender(fxAppender)
    log.addAppender(fileAppender)
    log.level = Level.TRACE

    log.trace("Saving log file to {}", fileAppender.file)

    return log
}

fun main(args: Array<String>) {
    val logger = configureLogger()
    logger.debug("Detected {}", System.getProperty("java.runtime.name"))
    logger.debug(
        "{} v{} by {}",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vm.version"),
        System.getProperty("java.vm.vendor")
    )
    logger.debug(
        "OS {} {} v{}",
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        System.getProperty("os.version")
    )
    logger.info("Starting launcher v{} ({})...", VERSION, GIT_SHA.substring(0,7))

    launch<LauncherApp>(args)
}
