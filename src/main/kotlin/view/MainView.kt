package view

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.SelectionMode
import javafx.scene.control.TabPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.RowConstraints
import javafx.scene.text.FontWeight
import mu.KotlinLogging
import tornadofx.*

class MainView : View("mc.z0ne.moe Launcher") {
    private val logger = KotlinLogging.logger {}

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

    override val root = vbox {
        setPrefSize(800.0, 600.0)
        minWidth = 600.0
        minHeight = 400.0

        tabpane {
            vgrow = Priority.ALWAYS
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            tab("News") {
                webview {
                    engine.load("https://mc.z0ne.moe")
                }
            }

            tab("Mods")

            tab("Logs") {
                vbox {
                    paddingAll = 5.0
                    spacing = 10.0

                    textarea {
                        vgrow = Priority.ALWAYS
                    }

                    buttonbar {
                        button("Copy")
                        button("Save")
                        button("Upload")
                    }
                }
            }

            tab("Accounts") {
                vbox {
                    paddingAll = 5.0
                    spacing = 10.0

                    listview<String> {
                        vgrow = Priority.ALWAYS
                        cellFormat {
                            text = it

                            style {
                                if (it == "Gamma") {
                                    fontWeight = FontWeight.BOLD
                                }
                            }
                        }

                        items.add("Alpha")
                        items.add("Beta")
                        items.add("Gamma")
                        selectionModel.selectionMode = SelectionMode.SINGLE
                    }

                    buttonbar {
                        vgrow = Priority.NEVER
                        button("Add new Account") {
                            action {
                                find<AddAccount>().openModal(owner = currentWindow!!, block = true)
                            }
                        }
                    }
                }
            }

            tab("Options") {
                form {
                    paddingAll = 5.0

                    fieldset {
                        legend = label("Minecraft") {
                            style {
                                fontScale = 1.1
                                fontWeight = FontWeight.BOLD
                            }
                        }

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
                        legend = label("Java") {
                            style {
                                fontScale = 1.1
                                fontWeight = FontWeight.BOLD
                            }
                        }

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

                    buttonbar {
                        button("Save") {
                            isDefaultButton = true
                        }
                        button("Cancel") {
                            isCancelButton = true
                        }
                        button("Reset to Defaults")
                    }
                }
            }

            tab("About") {
                vbox {
                    paddingAll = 5.0
                    spacing = 10.0

                    label("mcz Launcher") {
                        style {
                            fontScale = 1.5
                            fontWeight = FontWeight.BOLD
                        }
                    }

                    gridpane {
                        constraintsForColumn(0).prefWidth = 110.0
                        val rc = RowConstraints(30.0)
                        rc.valignment = VPos.TOP
                        rowConstraints.setAll(rc, rc, rc, rc)

                        row {
                            label("Version")
                            label("x.y.z-suffix (hash)")
                        }

                        row {
                            label("License")
                            label("Apache 2.0")
                        }

                        row {
                            label("Source Code")
                            hyperlink("https://github.com/mcz0ne/launcher") {
                                paddingAll = 0.0
                            }
                        }

                        row {
                            label("Authors")
                            vbox {
                                textflow {
                                    label("Kura Bloodlust (")
                                    hyperlink("https://github.com/cking") {
                                        paddingAll = 0.0
                                    }
                                    label(")")
                                }

                            }
                        }
                    }
                }
            }
        }

        hbox {
            vgrow = Priority.NEVER
            minHeight = 50.0
            maxHeight = 50.0

            label("l")
            label("m")
            label("r")
        }
    }
}