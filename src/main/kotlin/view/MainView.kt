package view

import controller.ConfigController
import controller.LauncherConfigController
import javafx.geometry.VPos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.RowConstraints
import javafx.scene.text.FontWeight
import javafx.stage.DirectoryChooser
import lib.Yggdrasil
import mu.KotlinLogging
import okio.internal.commonToUtf8String
import tornadofx.*
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream

class MainView : View("Launcher") {
    private var oldOut: PrintStream? = null
    private val logger = KotlinLogging.logger {}
    private val lcc: LauncherConfigController by inject()
    private val cc: ConfigController by inject()
    private val optModel = ConfigController.ConfigurationModel(cc.options)

    override val root = vbox {
        setPrefSize(800.0, 600.0)
        minWidth = 600.0
        minHeight = 400.0

        tabpane {
            vgrow = Priority.ALWAYS
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

            tab("News") {
                webview {
                    engine.load(lcc.news)
                }
            }

            tab("Logs") {
                vbox {
                    paddingAll = 5.0
                    spacing = 10.0

                    textarea {
                        vgrow = Priority.ALWAYS
                        id = "log"
                    }
                }
            }

            tab("Accounts") {
                vbox {
                    paddingAll = 5.0
                    spacing = 10.0

                    listview<Yggdrasil.Account> {
                        id = "account_list"
                        vgrow = Priority.ALWAYS
                        cellFormat {
                            text = "${it.username} (${it.email})"

                            style {
                                if (it.id == cc.selectedAccount?.id) {
                                    fontWeight = FontWeight.BOLD
                                }
                            }
                        }
                        items.bind(cc.accounts) { it }
                        selectionModel.selectionMode = SelectionMode.SINGLE
                    }

                    buttonbar {
                        vgrow = Priority.NEVER
                        button("Add new Account") {
                            action {
                                find<AddAccount>().openModal(owner = currentWindow!!, block = true)
                            }
                        }

                        button("Remove Account") {
                            id = "account_remove"
                        }

                        button("Use Account") {
                            id = "account_use"
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
                            textfield(optModel.width) {
                                filterInput { it.controlNewText.isInt() }
                            }
                        }

                        field("Window Height") {
                            textfield(optModel.height) {
                                filterInput { it.controlNewText.isInt() }
                            }
                        }

                        field {
                            checkbox("Enable Fullscreen", optModel.fullscreen)
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
                            button("...") {
                                action {
                                    val dc = DirectoryChooser()
                                    dc.title = "Select Java Home Directory"
                                    dc.initialDirectory = File(optModel.javaHome.value)
                                    val f = dc.showDialog(currentWindow!!)
                                    if (f != null) {
                                        try {
                                            optModel.javaHome.value = fixJRE(f).toString()
                                        } catch (ex: IOException) {
                                            val alert = Alert(Alert.AlertType.ERROR, "This is not a valid Java installation directory!", ButtonType.OK)
                                            alert.initOwner(currentWindow!!)
                                            alert.showAndWait()
                                        }
                                    }
                                }
                            }
                        }

                        field("Custom JVM Options") {
                            textarea(optModel.jvmOptions) {
                                isWrapText = true
                                prefRowCount = 3
                            }
                        }
                    }

                    buttonbar {
                        button("Save") {
                            isDefaultButton = true
                            action {
                                try {
                                    logger.debug("committing new options")
                                    optModel.commit {
                                        logger.debug("saving new options")
                                        optModel.item.update()
                                        cc.save()
                                    }
                                } catch (ex: IOException) {
                                    val alert = Alert(
                                        Alert.AlertType.ERROR,
                                        "Failed to save the changes!\n${ex.message}",
                                        ButtonType.OK
                                    )
                                    alert.initOwner(currentWindow!!)
                                    alert.showAndWait()
                                }
                            }
                        }
                        button("Cancel") {
                            isCancelButton = true

                            action {
                                optModel.rollback()
                            }
                        }
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

    override fun onUndock() {
        System.setOut(oldOut)

        super.onUndock()
    }

    override fun onDock() {
        oldOut = System.out
        System.setOut(object : PrintStream(oldOut as OutputStream) {
            override fun write(arr: ByteArray) {
                super.write(arr)

                val log = root.lookup("#log") as TextArea
                log.appendText(arr.commonToUtf8String())
            }
        })

        super.onDock()

        val list = root.lookup("#account_list")!! as ListView<*>

        list.setOnMouseClicked {
            if (it.clickCount >= 2) {
                val acc = list.selectedItem
                if (acc != null) {
                    cc.selectedAccount = acc as Yggdrasil.Account?
                    list.refresh()
                }
            }
        }

        root.lookup("#account_remove")!!.setOnMouseClicked {
            val acc = list.selectedItem as Yggdrasil.Account?
            if (acc != null) {
                cc.removeAccount(acc.id)
                list.refresh()
            }
        }

        root.lookup("#account_use")!!.setOnMouseClicked {
            val acc = list.selectedItem
            if (acc != null) {
                cc.selectedAccount = acc as Yggdrasil.Account?
                list.refresh()
            }
        }
    }

    fun fixJRE(f: File): File {
        val jexe = if (System.getProperty("os.name").toLowerCase().contains("win")) {
            "java.exe"
        } else {
            "java"
        }

        return when {
            File(f, "bin/$jexe").exists() -> f
            File(f, jexe).exists() -> f.parentFile
            else -> throw IOException("invalid java home directory")
        }
    }
}