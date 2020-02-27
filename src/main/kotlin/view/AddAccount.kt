package view

import controller.AccountsController
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import lib.Yggdrasil
import mu.KotlinLogging
import tornadofx.*
import tornadofx.Fieldset

class AddAccount : View("Login to Minecraft") {
    private val logger = KotlinLogging.logger {}
    private val accounts: AccountsController by inject()

    private val model = object : ViewModel() {
        val username = bind { SimpleStringProperty() }
        val password = bind { SimpleStringProperty() }
    }

    override val root = form {
        fieldset {
            label("Your password wont be stored on the hard drive.\nInstead we will only keep a dynamic, non reversable ID given out by Mojang.")

            field("Username") {
                textfield(model.username)
            }

            field("Password") {
                passwordfield(model.password)
            }

            buttonbar {
                vgrow = Priority.NEVER
                button("Login") {
                    isDefaultButton = true

                    action {
                        logger.debug("verifying login details")
                        model.commit {
                            try {
                                logger.debug("logging in to minecraft")
                                accounts.authenticate(model.username.value, model.password.value)
                                logger.info("logged in to the account <{}>", model.username.value)
                                currentStage?.close()
                            } catch (ex: Yggdrasil.ForbiddenException) {
                                logger.warn("failed to login!")
                                val alert = Alert(Alert.AlertType.ERROR, ex.message, ButtonType.OK)
                                alert.initOwner(currentWindow!!)
                                alert.showAndWait()
                            }
                        }
                    }
                }

                button("Abort") {
                    isCancelButton = true

                    action {
                        currentStage?.close()
                    }
                }
            }
        }
    }
}
