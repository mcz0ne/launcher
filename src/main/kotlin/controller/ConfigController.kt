package controller

import javafx.collections.ObservableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import lib.Yggdrasil
import mu.KotlinLogging
import tornadofx.Controller
import tornadofx.asObservable
import java.io.File
import java.util.*


class ConfigController : Controller() {
    @Serializable
    data class Configuration(
        var clientToken: String = UUID.randomUUID().toString(),

        val accounts: MutableList<Yggdrasil.Account> = mutableListOf(),
        var selectedAccount: String = ""
    )

    companion object {
        private val json = Json(JsonConfiguration.Stable.copy(prettyPrint = true))
    }

    private val launcherConfigController: LauncherConfigController by inject()
    private val appConfig: Configuration
    private val logger = KotlinLogging.logger {}
    private val ygg: Yggdrasil

    private val accountsObserver: ObservableList<Yggdrasil.Account>

    init {
        appConfig = try {
            val f = File(launcherConfigController.folder(), "config.json")
            val t = f.inputStream().bufferedReader().readText()
            json.parse(Configuration.serializer(), t)
        } catch (e: Exception) {
            Configuration()
        }

        ygg = Yggdrasil(appConfig.clientToken)
        accountsObserver = appConfig.accounts.asObservable()
        save()
    }

    private fun save() {
        logger.debug("ensuring config path <{}>", launcherConfigController.folder())
        launcherConfigController.folder().mkdirs()

        logger.debug("saving configuration")
        val s = json.stringify(Configuration.serializer(), appConfig)
        File(launcherConfigController.folder(), "config.json").writeText(s)
    }

    private fun verifyAccount(username: String): Boolean {
        val acc = accountsObserver.find { it.email == username }
        var ok = false
        try {
            if (acc != null) {
                if (!ygg.validate(acc.accessToken)) {
                    // no need to call logout
                    accountsObserver.remove(acc)

                    val newAcc = ygg.refresh(acc)
                    accountsObserver.add(newAcc)
                    ok = true
                }
            }
        } finally {
            save()
            return ok
        }
    }

    fun addAccount(username: String, password: String) {
        if (verifyAccount(username)) {
            return
        }

        accountsObserver.add(ygg.authenticate(username, password))
        if (accountsObserver.count() == 1) {
            appConfig.selectedAccount = accountsObserver[0].id
        }

        save()
    }

    fun removeAccount(id: String) {
        val acc =
            accountsObserver.find { it.email == id || it.username == id || it.accessToken == id }

        if (acc != null) {
            ygg.invalidate(acc.accessToken)
            accountsObserver.remove(acc)

            save()
        }
    }

    var clientToken: String
        get() = appConfig.clientToken
        set(v) {
            appConfig.clientToken = v
            save()
        }

    var selectedAccount: Yggdrasil.Account?
        get() = accountsObserver.find{it.id == appConfig.selectedAccount}
        set(v) {
            if (v == null || !accountsObserver.contains(v)) {
                throw IndexOutOfBoundsException("selectedAccount")
            }
            appConfig.selectedAccount = v.id
            save()
        }

    val accounts: ObservableList<Yggdrasil.Account>
        get() = accountsObserver
}