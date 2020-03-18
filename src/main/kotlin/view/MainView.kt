package view

import BUILD_DATE
import DATA_DIR
import GIT_SHA
import JAVA_EXECUTABLE
import LAUNCHER
import VERSION
import controller.Minecraft
import controller.ProfilesModel
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import lib.Dialog
import lib.file
import lib.join
import lib.minecraft.LauncherProfileAuthenticationEntry
import mu.KotlinLogging
import tornadofx.*
import java.awt.Desktop
import java.net.URI

class MainView : View("${LAUNCHER.name} Launcher") {
    private val logger = KotlinLogging.logger {}

    private val minecraft: Minecraft by inject()
    private val mcInstallStatus = TaskStatus()
    private val profiles: ProfilesModel by inject()

    private var accountBox: ComboBox<Pair<String, LauncherProfileAuthenticationEntry>> by singleAssign()
    private var launchButton: Button by singleAssign()

    init {
        mcInstallStatus.completed.addListener { _, _, completed ->
            if (completed) {
                profiles.reload()
                accountBox.items.bind(profiles.accounts) { key, value -> Pair(key, value) }
                accountBox.valueProperty().bind(profiles.selectedAccount)

                if (app.parameters.raw.contains("launch")) {
                    launchMC()
                }
            }
        }
    }

    override val root = vbox {
        setPrefSize(800.0, 600.0)
        minWidth = 600.0
        minHeight = 400.0

        webview {
            vgrow = Priority.ALWAYS
            engine.load(LAUNCHER.news.toString())
            paddingRight = 5.0
        }

        hbox {
            vgrow = Priority.NEVER
            hgrow = Priority.ALWAYS
            minHeight = 75.0
            maxHeight = 75.0
            paddingAll = 10.0

            vbox {
                spacing = 5.0

                combobox<Pair<String, LauncherProfileAuthenticationEntry>> {
                    accountBox = this

                    minWidth = 150.0
                    maxWidth = 150.0

                    disableProperty().bind(mcInstallStatus.completed.not())

                    cellFormat { acc ->
                        text = "${acc.second.profiles.values.first().displayName} (${acc.second.username})"
                    }

                    valueProperty().addListener { _, _, new -> profiles.selectedID = new.first }
                }

                hbox {
                    button("Add") {
                        action {
                            profiles.interactiveLogin(primaryStage)
                        }
                    }

                    button("Remove") {
                        action {
                            profiles.logout()
                        }
                    }
                }
            }

            vbox {
                spacing = 5.0
                alignment = Pos.CENTER
                hgrow = Priority.ALWAYS
                vgrow = Priority.ALWAYS
                visibleWhen(mcInstallStatus.running)

                label(mcInstallStatus.title) {
                    style {
                        fontWeight = FontWeight.BOLD
                    }
                }
                label(mcInstallStatus.message)
                progressbar(mcInstallStatus.progress)
            }

            vbox {
                spacing = 5.0
                alignment = Pos.CENTER_RIGHT

                button("Launch Minecraft") {
                    launchButton = this
                    disableProperty().bind(mcInstallStatus.completed.not())

                    action {
                        launchMC()
                    }
                }

                button("Info") {
                    action {
                        val openButton = ButtonType("Open GitHub", ButtonBar.ButtonData.OK_DONE)
                        Dialog.information(
                            "About MCZ Launcher",
                            """
                        MCZ Version $VERSION (${GIT_SHA.substring(0, 7)})
                        Compiled on $BUILD_DATE
                        Released under the Apache 2.0 License
                        """.trimIndent(),
                            ButtonType.OK,
                            openButton,
                            owner = primaryStage
                        ) {
                            if (this.result == openButton) {
                                if (!Desktop.isDesktopSupported()) {
                                    logger.error("Desktop is not supported!")
                                    return@information
                                }

                                val d = Desktop.getDesktop()
                                if (!d.isSupported(Desktop.Action.BROWSE)) {
                                    logger.error("Browsing links unsupported!")
                                    return@information
                                }

                                task {
                                    d.browse(URI("https://github.com/mcz0ne/launcher"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBeforeShow() {
        super.onBeforeShow()
        minecraft.install(DATA_DIR, mcInstallStatus)
    }

    private fun launchMC() {
        if (profiles.accounts.size == 0 || profiles.selectedAccount.get() == null || !profiles.verifyAccount()) {
            if (!profiles.interactiveLogin(primaryStage, true)) {
                Dialog.error(
                    "Failed to login",
                    "Failed to login to Minecraft. Please verify your login credentials!",
                    title = "Authentication error",
                    owner = primaryStage
                )
                return
            }
        }

        val java = System.getProperty("java.home").file().join("bin", JAVA_EXECUTABLE).toString()

        runAsync {
            logger.debug("running serversync")
            ProcessBuilder(java, "-jar", DATA_DIR.join("serversync.jar").toString(), "progress-only")
                .directory(DATA_DIR)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()

            val acc = profiles.selectedAccount.get()!!
            val yggAcc = acc.second.toYggdrasil(acc.first)
            logger.debug("preparing launch args")
            val args = minecraft.launchArgs(DATA_DIR, yggAcc)
            logger.trace("launch args received:")
            args.forEach {
                logger.trace("> {}", it)
            }

            logger.info("Starting game as {}", yggAcc.username)

            ProcessBuilder(
                listOf(
                    java,
                    "-Xms" + minecraft.profile.minMem,
                    "-Xmx" + minecraft.profile.maxMem
                ) + minecraft.profile.javaOpts + args
            )
                .directory(DATA_DIR)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
        }
    }
}