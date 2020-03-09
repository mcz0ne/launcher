package app

import app.config.Accounts
import app.config.Instance
import javafx.stage.Stage
import joptsimple.OptionParser
import joptsimple.OptionSet
import lib.FXAppender
import lib.file
import lib.join
import mu.KotlinLogging
import runtimeDirectories
import style.Styles
import tornadofx.App
import tornadofx.FX
import tornadofx.UIComponent
import view.MainView
import java.io.File
import java.io.FileNotFoundException

class LauncherApp : App(MainView::class, Styles::class) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    internal lateinit var launcherConfig: LauncherConfig
    private lateinit var parsedParameters: OptionSet

    private var _accounts: Accounts? = null
    internal val accounts: Accounts
        get() {
            if (_accounts == null) {
                val accountsFile = File(runtimeDirectories.configDir.file(), "accounts.json")

                _accounts =try {
                    Accounts.load(accountsFile)
                } catch (ex: FileNotFoundException) {
                    Accounts()
                }
            }

            return _accounts!!
        }

    private var _instance: Instance? = null
    internal val instance: Instance
        get() {
            if (_instance == null) {
                val accountsFile = runtimeDirectories.configDir.file().join(launcherConfig.id, "config.json")
                _instance = Instance.load(accountsFile)
            }

            return _instance!!
        }

    override fun start(stage: Stage) {
        logger.debug("parsing arguments: {}", this.parameters.raw)
        val parser = OptionParser()
        parser.allowsUnrecognizedOptions()
        parser.acceptsAll(listOf("profile", "p"), "Load a custom json profile instead of the jar provided one")
            .withRequiredArg()
        parser.acceptsAll(listOf("help", "h", "?"), "Print this help message").forHelp()
        parsedParameters = parser.parse(*this.parameters!!.raw.toTypedArray())

        try {
            if (parsedParameters.hasArgument("profile")) {
                val profilePath = parsedParameters.valueOf("profile") as String
                logger.warn("using profile override: {}", profilePath)
                val f = File(profilePath)
                launcherConfig = LauncherConfig.loadFromFile(f)
            }
        } catch (ex: Exception) {
            logger.error("failed to load profile override, falling back to jar profile", ex)
        }

        if (!::launcherConfig.isInitialized) {
            launcherConfig = LauncherConfig.loadFromJAR()
        }

        runtimeDirectories.configDir.file().mkdirs()
        logger.debug("launcher configured: {} {}",launcherConfig, runtimeDirectories)

        super.start(stage)
    }

    override fun onBeforeShow(view: UIComponent) {
        super.onBeforeShow(view)
    }

    override fun stop() {
        super.stop()
    }
}