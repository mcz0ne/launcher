package view

import BUILD_DATE
import GIT_SHA
import JAVA_EXECUTABLE
import VERSION
import app.LauncherApp
import controller.UpdateController
import javafx.concurrent.Task
import javafx.geometry.VPos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.RowConstraints
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import lib.Dialog
import lib.file
import lib.join
import lib.url
import lib.yggdrasil.Account
import lib.yggdrasil.BadRequestException
import mu.KotlinLogging
import org.controlsfx.control.TaskProgressView
import tornadofx.*
import java.awt.Desktop
import java.io.File
import java.io.IOException

class MainView : View("Launcher") {
    private val logger = KotlinLogging.logger {}

    private val updateController: UpdateController by inject()

    private var accountList: ListView<Account> by singleAssign()
    var taskProgressView: TaskProgressView<Task<Unit>> by singleAssign()

    private val launcherApp: LauncherApp
        get() = super.app as LauncherApp

    init {
        subscribe<UpdateController.UpdateAvailable> {
            if (launcherApp.instance.alwaysUpdate) {
                updateController.update()
            } else {
                val alwaysButton = ButtonType("Always")
                Dialog.confirmation(
                    "Modpack Update",
                    "${launcherApp.launcherConfig.name} has been updated to ${updateController.modpack!!.version}.\nDo you want to update now? (You can only launch Minecraft after updating)",
                    ButtonType.YES, alwaysButton, ButtonType.NO,
                    owner = currentStage,
                    title = "Modpack Update"
                ) {
                    // dont do anythang
                    if (it == ButtonType.NO) {
                        return@confirmation
                    }

                    if (it == alwaysButton) {
                        launcherApp.instance.alwaysUpdate = true
                        launcherApp.instance.save()
                    }

                    updateController.update()
                }
            }
        }

        title = "${launcherApp.launcherConfig.name} Launcher"
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
                    engine.load(launcherApp.launcherConfig.news)
                }
            }

            tab("Jobs") {
                // needs to be done manually
                taskProgressView = TaskProgressView()
                taskProgressView.paddingAll = 5.0
                taskProgressView.isRetainTasks = true
                add(taskProgressView)
            }

            tab("Accounts") {
                vbox {
                    paddingAll = 5.0
                    spacing = 10.0

                    listview<Account> {
                        accountList = this
                        vgrow = Priority.ALWAYS
                        cellFormat { acc ->
                            text = if (acc.username == acc.email) {
                                // legacy account, not using a mail address
                                acc.username
                            } else {
                                "${acc.username} (${acc.email})"
                            }

                            style {
                                if (launcherApp.accounts.isActive(acc)) {
                                    fontWeight = FontWeight.BOLD
                                }
                            }
                        }

                        items.bind(launcherApp.accounts.set) { it }
                        selectionModel.selectionMode = SelectionMode.SINGLE
                    }

                    buttonbar {
                        vgrow = Priority.NEVER
                        button("Add new Account") {
                            action {
                                launcherApp.accounts.interactiveLogin(currentStage!!)
                            }
                        }

                        button("Remove Account") {
                            action {
                                launcherApp.accounts.logout()
                                accountList.refresh()
                            }
                        }

                        button("Use Account") {
                            action {
                                launcherApp.accounts.activeAccount = accountList.selectedItem
                                accountList.refresh()
                            }
                        }
                    }
                }
            }

            tab("Options") {
                form {
                    paddingAll = 5.0

                    fieldset {
                        legend = label("Launcher") {
                            style {
                                fontScale = 1.1
                                fontWeight = FontWeight.BOLD
                            }
                        }

                        field {
                            checkbox("Always update modpack without asking", launcherApp.instance.alwaysUpdateProperty)
                        }
                    }

                    fieldset {
                        legend = label("Minecraft") {
                            style {
                                fontScale = 1.1
                                fontWeight = FontWeight.BOLD
                            }
                        }

                        field("Window Width") {
                            textfield(launcherApp.instance.minecraftWidthProperty) {
                                filterInput { it.controlNewText.isInt() }
                            }
                        }

                        field("Window Height") {
                            textfield(launcherApp.instance.minecraftHeightProperty) {
                                filterInput { it.controlNewText.isInt() }
                            }
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
                            textfield(launcherApp.instance.javaMinMemProperty)
                        }

                        field("Maximum Memory") {
                            textfield(launcherApp.instance.javaMaxMemProperty)
                        }

                        field("Java Home Path") {
                            textfield(launcherApp.instance.javaHomeProperty)
                            button("...") {
                                action {
                                    val f = chooseDirectory(
                                        "Select Java Home Directory",
                                        launcherApp.instance.javaHomeProperty.value.file(),
                                        primaryStage
                                    )
                                    if (f != null) {
                                        try {
                                            launcherApp.instance.javaHomeProperty.value = fixJRE(f).toString()
                                        } catch (ex: IOException) {
                                            Dialog.error(
                                                "Failed to set java home",
                                                "This is not a valid Java installation directory!",
                                                ButtonType.OK,
                                                owner = primaryStage
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        field("Custom JVM Options") {
                            textarea(launcherApp.instance.javaOptionsProperty) {
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
                                    /*launcherApp.instance.commit {
                                         logger.debug("saving new options")
                                         //optModel.item.update()*/
                                    launcherApp.instance.save()
                                    //}
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
                                //                                optModel.rollback()
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
                            label("$VERSION (${GIT_SHA.substring(0, 7)})")
                        }

                        row {
                            label("Compiled")
                            label(BUILD_DATE)
                        }

                        row {
                            label("License")
                            label("Apache 2.0")
                        }

                        row {
                            label("Source Code")
                            hyperlink("https://github.com/mcz0ne/launcher") {
                                paddingAll = 0.0

                                action {
                                    openLink(text)
                                }
                            }
                        }

                        row {
                            label("Authors")
                            vbox {
                                textflow {
                                    label("Kura Bloodlust (")
                                    hyperlink("https://github.com/cking") {
                                        paddingAll = 0.0

                                        action {
                                            openLink(text)
                                        }
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
            hgrow = Priority.ALWAYS
            minHeight = 30.0
            maxHeight = 30.0

            pane {
                hgrow = Priority.ALWAYS
            }

            textflow {
                paddingAll = 6.0
                label("Selected Minecraft Account: ")
                label(launcherApp.accounts.activeAccount?.username ?: "unknown...") {
                    launcherApp.accounts.activeAccountProperty.addListener { _, _, acc ->
                        if (acc == null) {
                            this.text = "unknown..."
                            this.style {
                                fontStyle = FontPosture.ITALIC
                                fontWeight = FontWeight.NORMAL
                            }
                        } else {
                            this.text = acc.username
                            this.style {
                                fontStyle = FontPosture.REGULAR
                                fontWeight = FontWeight.BOLD
                            }
                        }
                    }
                    style {
                        if (launcherApp.accounts.activeAccount == null) {
                            fontStyle = FontPosture.ITALIC
                        } else {
                            fontWeight = FontWeight.BOLD
                        }
                    }
                }
            }
            button("Launch Minecraft") {
                isDisable = true
                updateController.canLaunchProperty.addListener { _, old, new ->
                    logger.trace("can launch changed from {} to {}", old, new)
                    this.isDisable = true != new
                }

                action {
                    var acc = launcherApp.accounts.activeAccount
                    if ((acc == null || try {
                            launcherApp.accounts.verify(acc) != true
                        } catch (ex: BadRequestException) {
                            true
                        }) && !launcherApp.accounts.interactiveLogin(currentStage!!, acc?.email ?: "")
                    ) {
                        Dialog.error(
                            "Failed to login",
                            "Failed to login to Minecraft. Please verify your login credentials!",
                            ButtonType.OK,
                            title = "Authentication error",
                            owner = currentStage
                        )
                        return@action
                    }
                    acc = launcherApp.accounts.activeAccount!!

                    logger.debug("preparing launch args")
                    val args = updateController.play(acc)
                    logger.trace("launch args received:")
                    args.forEach {
                        logger.trace("> {}", it)
                    }

                    logger.info("Starting game as {}", acc.username)

                    ProcessBuilder(
                        (listOf(
                            launcherApp.instance.javaHome.file().join("bin", JAVA_EXECUTABLE).toString(),
                            "-Xms" + launcherApp.instance.javaMinMem,
                            "-Xmx" + launcherApp.instance.javaMaxMem
                        ) + launcherApp.instance.javaOptions.split(" ") + args)
                    )
                        .directory(updateController.dataDir)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()
                }
            }
        }
    }

    override fun onDock() {
        accountList.setOnMouseClicked {
            if (it.clickCount >= 2) {
                val acc = accountList.selectedItem
                if (acc == null) {
                    launcherApp.accounts.interactiveLogin(primaryStage)
                } else {
                    logger.trace("clicked {} times on {}", it.clickCount, acc)
                    launcherApp.accounts.activeAccount = acc
                    accountList.refresh()
                }
            }
        }

        updateController.check()

        super.onDock()
    }

    private fun fixJRE(f: File): File {
        return when {
            f.join("bin", JAVA_EXECUTABLE).exists() -> f
            f.join(JAVA_EXECUTABLE).exists() -> f.parentFile
            else -> throw IOException("invalid java home directory")
        }
    }

    private fun openLink(link: String) {
        logger.trace("Prepaing to open: {}", link)
        if (!Desktop.isDesktopSupported()) {
            logger.error("Desktop is not supported!")
            return
        }

        val d = Desktop.getDesktop()
        if (!d.isSupported(Desktop.Action.BROWSE)) {
            logger.error("Browsing links unsupported!")
            return
        }

        logger.trace("Opening link {}", link)
        task {
            d.browse(link.url().toURI())
        }
    }

}