package app

import javafx.stage.Stage
import style.Styles
import tornadofx.App
import view.MainView

class LauncherApp : App(MainView::class, Styles::class) {
}