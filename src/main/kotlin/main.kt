import app.LauncherApp
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import lib.LauncherProperties
import lib.OS
import lib.join
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tornadofx.launch
import java.io.File

val LAUNCHER = LauncherProperties()
lateinit var DATA_DIR: File

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

fun configureLogger(verbose: Boolean): Logger {
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    val logFile = DATA_DIR.join("app.log")
    logFile.delete()

    val consoleAppender = ConsoleAppender<ILoggingEvent>()
    consoleAppender.name = "console"
    consoleAppender.isWithJansi = !OS.detect().isWin
    consoleAppender.target = "System.out"
    consoleAppender.encoder = newPattern(lc, true)
    consoleAppender.context = lc
    consoleAppender.start()

    val fileAppender = FileAppender<ILoggingEvent>()
    fileAppender.name = "file"
    fileAppender.encoder = newPattern(lc)
    fileAppender.isAppend = false
    fileAppender.file = logFile.toString()
    fileAppender.context = lc
    fileAppender.start()

    val log = lc.getLogger("root")
    log.isAdditive = false
    log.detachAndStopAllAppenders()
    log.addAppender(consoleAppender)
    log.addAppender(fileAppender)
    log.level = if (verbose) {
        Level.TRACE
    } else {
        Level.INFO
    }

    log.trace("Saving log file to {}", fileAppender.file)

    return log
}

fun main(args: Array<String>) {
    LauncherApp::class.java.getResourceAsStream("/launcher.properties").use { LAUNCHER.load(it) }
    DATA_DIR = OS.detect().dataDir(LAUNCHER.id)

    val logger = configureLogger(args.contains("-v"))
    logger.debug("Detected {}", System.getProperty("java.runtime.name"))
    logger.trace(
        "{} v{} by {}",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vm.version"),
        System.getProperty("java.vm.vendor")
    )
    logger.trace(
        "OS {} {} v{}",
        System.getProperty("os.name"),
        System.getProperty("os.arch"),
        System.getProperty("os.version")
    )
    logger.info("Starting launcher v{} ({})...", VERSION, GIT_SHA.substring(0, 7))
    logger.trace("Config: {}", LAUNCHER)
    logger.debug("Working directory: {}", DATA_DIR)

    launch<LauncherApp>(args)
}
