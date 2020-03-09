package app.config

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.*
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Stage
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import lib.Util
import lib.yggdrasil.Account
import lib.yggdrasil.ForbiddenException
import lib.yggdrasil.Yggdrasil
import mu.KotlinLogging
import tornadofx.*
import view.AddAccount
import java.io.File
import java.util.*

@Serializable
data class Accounts(
    private val list: MutableSet<Account> = mutableSetOf(),
    private var active: String? = null,
    val clientToken: String = UUID.randomUUID().toString(),

    @Transient private var location: File? = null
) {
    companion object {
        private val logger = KotlinLogging.logger {}

        fun load(location: File?): Accounts {
            return if (location == null) {
                Accounts()
            } else {
                val content = location.readText()
                val accounts = Util.json.parse(serializer(), content)
                accounts.location = location
                accounts
            }
        }
    }

    @Transient
    val set: ObservableSet<Account> = list.asObservable()

    @Transient
    private var yggdrasil: Yggdrasil? = null

    init {
        reload()
        logger.debug("initializing yggdrasil with {}", clientToken)
        yggdrasil = Yggdrasil(clientToken)
    }

    var activeAccount: Account?
        get() {
            return list.find { it.id == active }
        }
        set(v) {
            val save = active == null || active != v?.id
            active = v?.id ?: throw NullPointerException("Set active account to an account")
            if (save) this.save()
        }

    fun reload(newLocation: File? = null) {
        if (newLocation != null) {
            location = newLocation
        }

        if (location == null) {
            // cant reload whats not there
            return
        }

        val newAccounts = load(location)

        // dont logout accounts, assume all accounts are invalid by default
        set.clear()
        set.addAll(newAccounts.set)

        active = newAccounts.active
    }

    fun save(newLocation: File? = null) {
        if (newLocation != null) {
            location = newLocation
        }

        logger.trace("saving new accounts json to {}", location)
        val content = Util.json.stringify(serializer(), this)
        location!!.writeText(content)
    }

    fun isActive(acc: Account): Boolean {
        return active == acc.id
    }

    fun get(id: String?): Account? = set.find { it.id == id }

    fun interactiveLogin(stage: Stage, username: String = ""): Boolean {
        logger.trace("opening add account modal")
        var view = find<AddAccount>()
        if (username != "") {
            view = view.with(username, "Failed to verify the saved credentials, please login again.")
        }
        view.openModal(owner = stage, block = true)
        return if (!view.submitted) {
            logger.debug("aborted adding an account")
            false
        } else {
            try {
                login(view.result!!.username, view.result!!.password)
                true
            } catch (ex: ForbiddenException) {
                alert(
                    type = Alert.AlertType.ERROR,
                    owner = stage,
                    header = "Failed to login to " + view.result!!.username,
                    content = ex.message!!,
                    buttons = *arrayOf(ButtonType.OK)
                )
                false
            }
        }
    }

    fun login(username: String, password: String) {
        val acc = yggdrasil!!.authenticate(username, password)
        logger.trace("logged in {} as {}", acc.id, acc.username)
        set.removeIf { it.id == acc.id }
        set.add(acc)
        if (set.size == 1 || activeAccount == null) {
            activeAccount = set.first()
        }

        save()
    }

    fun logout(acc: Account? = null) {
        val selAccount = acc ?: activeAccount ?: return

        yggdrasil!!.invalidate(selAccount)
        if (activeAccount == selAccount) {
            activeAccount = set.first()
        }

        set.remove(selAccount)
        save()
    }

    fun verify(acc: Account?): Boolean? {
        val selAcc = acc ?: activeAccount ?: return false

        return if (yggdrasil!!.validate(selAcc)) {
            true
        } else {
            try {
                val refreshed = yggdrasil!!.refresh(selAcc)
                this.set.remove(selAcc)
                this.set.add(refreshed)
                true
            } catch (ex: ForbiddenException) {
                false
            }
        }
    }
}