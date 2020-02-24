package view

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import mu.KotlinLogging
import tornadofx.*

class MainView:View("mc.z0ne.moe Launcher") {
    private val logger = KotlinLogging.logger{}

    private val optModel = object : ViewModel() {
        val minMem = bind { SimpleStringProperty() }
        val maxMem = bind { SimpleStringProperty() }
        val javaHome = bind { SimpleStringProperty() }
        val jvmOpts = bind { SimpleStringProperty() }
    }

    private val mcModel = object : ViewModel() {
        val fullscreen = bind { SimpleBooleanProperty() }
        val width = bind { SimpleIntegerProperty() }
        val height = bind { SimpleIntegerProperty() }
    }

    override val root = borderpane {
        setPrefSize(800.0, 600.0)
        minWidth = 600.0
        minHeight = 400.0

        top = tabpane {
            useMaxWidth = true
            useMaxHeight = true
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            tab("News") {
                webview {
                    useMaxWidth = true
                    useMaxHeight = true

                    engine.load("https://mc.z0ne.moe")
                }
            }
            tab("Mods")
            tab("Progress")
            tab("Logs")

            tab("Accounts") {

            }

            tab("Options") {
                form {
                    fieldset {
                        legend = label("Minecraft")

                        field("Window Width") {
                            textfield(mcModel.width)
                        }

                        field("Window Height") {
                            textfield(mcModel.height)
                        }

                        field {
                            checkbox("Enable Fullscreen", mcModel.fullscreen)
                        }
                    }

                    fieldset {
                        legend = label("Java")

                        field("Minimum Memory") {
                            textfield(optModel.minMem)
                        }

                        field("Maximum Memory") {
                            textfield(optModel.maxMem)
                        }

                        field("Java Home Path") {
                            textfield(optModel.javaHome)
                            button("...")
                        }

                        field("Custom JVM Options") {
                            textarea(optModel.jvmOpts)
                        }
                    }

                    hbox {
                        button("Save") {
                            isDefaultButton = true
                        }
                        button("Cancel") {
                            isCancelButton = true
                        }
                        button("Reset to Defaults") {}
                    }
                }
            }
        }

        bottom = hbox {
            useMaxWidth = true
            minHeight = 200.0
            maxHeight = 200.0

            label("l")
            label("m")
            label("r")
        }
    }
}