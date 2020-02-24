import tornadofx.*
import app.LauncherApp
import mu.KotlinLogging

private val logger = KotlinLogging.logger{}

fun main(args: Array<String>) {
    logger.info("Starting launcher...")
    launch<LauncherApp>(args)
}
