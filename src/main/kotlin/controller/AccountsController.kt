package controller

import lib.Yggdrasil
import tornadofx.Controller

class AccountsController : Controller() {
    private val configController: ConfigController by inject()
    private val yggdrasil = Yggdrasil(configController.clientToken)

    fun authenticate(username: String, password: String) {
        yggdrasil.authenticate(username, password)
    }
}