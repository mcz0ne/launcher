package app

import DATA_DIR
import javafx.stage.Stage
import lib.join
import lib.minecraft.LauncherProfiles
import mu.KotlinLogging
import tornadofx.App
import view.MainView

class LauncherApp : App(MainView::class) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun start(stage: Stage) {
        logger.trace("Checking for <launcher_profiles.json>...")
        if (!DATA_DIR.join(LauncherProfiles.FILENAME).exists()) {
            logger.info("<launcher_profiles.json> not found, creating!")
            LauncherProfiles().save(DATA_DIR.join(LauncherProfiles.FILENAME))
        }

        super.start(stage)
    }

}