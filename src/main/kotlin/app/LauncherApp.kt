package app

import controller.LauncherConfigController
import style.Styles
import tornadofx.App
import view.MainView
import java.util.*

class LauncherApp : App(MainView::class, Styles::class) {
    private val launcherConfig: LauncherConfigController by inject()

    init {
        val properties = Properties()
        properties.load(this.resources.stream("/launcher.properties"))
        launcherConfig.fromProperties(properties)
    }
}