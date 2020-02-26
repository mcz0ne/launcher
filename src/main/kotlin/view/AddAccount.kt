package view

import controller.AccountsController
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.Priority
import javafx.scene.text.FontWeight
import tornadofx.*
import tornadofx.Fieldset

class AddAccount : View("Login to Minecraft") {
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
                        model.commit {
                            accounts.authenticate(model.username.value, model.password.value)
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
