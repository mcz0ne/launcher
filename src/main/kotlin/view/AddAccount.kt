package view

import javafx.scene.control.Label
import javafx.scene.layout.Priority
import mu.KotlinLogging
import tornadofx.*

class AddAccount : Fragment("Login to Minecraft") {
    class Result {
        var username: String by property()
        internal fun usernameProperty() = getProperty(Result::username)

        var password: String by property()
        internal fun passwordProperty() = getProperty(Result::password)
    }

    private val logger = KotlinLogging.logger {}
    private val model = object : ItemViewModel<Result>() {
        init {
            item = Result()
        }

        val username = bind { item?.usernameProperty() }
        val password = bind { item?.passwordProperty() }
    }

    private var formLabel: Label by singleAssign()
    private var labelText: String? = null

    fun with(username: String = "", message: String = ""): AddAccount {
        model.item.username = username
        labelText = message
        return this
    }

    override fun onBeforeShow() {
        formLabel.text = labelText
            ?: "Your password wont be stored on the hard drive.\nInstead we will only keep a dynamic, non reversable ID given out by Mojang."
        labelText = null

        super.onBeforeShow()
    }

    override fun onUndock() {
        formLabel.text = ""
        super.onUndock()
    }

    var submitted = false
        private set

    val result: Result?
        get() = if (submitted) model.item else null

    override val root = form {
        fieldset {
            label {
                formLabel = this
            }

            field("Username") {
                textfield(model.username).required(message = "Provide your login username")
            }

            field("Password") {
                passwordfield(model.password).required(message = "Provide your password")
            }

            buttonbar {
                vgrow = Priority.NEVER
                button("Login") {
                    isDefaultButton = true

                    action {
                        try {
                            logger.debug("trying to commit")
                            submitted = model.commit()
                            if (submitted) {
                                currentStage?.close()
                            }
                        } catch (ex: Exception) {
                            logger.error("unkown ex", ex)
                        }
                    }
                }

                button("Abort") {
                    isCancelButton = true

                    action {
                        submitted = false
                        currentStage?.close()
                    }
                }
            }
        }
    }
}
