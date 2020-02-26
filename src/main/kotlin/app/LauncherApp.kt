package app

import lib.LauncherConfig
import style.Styles
import tornadofx.App
import view.MainView
import java.util.*

class LauncherApp : App(MainView::class, Styles::class) {
    private val launcherConfig: LauncherConfig by inject()

    init {
        val properties = Properties()
        properties.load(this.resources.stream("/launcher.properties"))
        launcherConfig.fromProperties(properties)
    }
}