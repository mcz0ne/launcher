package controller

import DATA_DIR
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableMap
import javafx.scene.control.Alert
import javafx.stage.Stage
import lib.join
import lib.minecraft.LauncherProfileAuthenticationEntry
import lib.minecraft.LauncherProfileSelectedUser
import lib.minecraft.LauncherProfiles
import lib.yggdrasil.ForbiddenException
import lib.yggdrasil.Yggdrasil
import tornadofx.Controller
import tornadofx.alert
import tornadofx.asObservable
import view.AddAccount

class ProfilesModel : Controller() {
    private lateinit var profiles: LauncherProfiles
    private lateinit var yggdrasil: Yggdrasil

    lateinit var accounts: ObservableMap<String, LauncherProfileAuthenticationEntry>
        private set

    var selectedID: String?
        get() = profiles.selectedUser?.account
        set(v) {
            if (profiles.selectedUser == null || v == null) {
                profiles.selectedUser = LauncherProfileSelectedUser()
            }

            if (v == null) {
                return
            }

            profiles.selectedUser!!.account = v
            profiles.selectedUser!!.profile = profiles.authenticationDatabase.getValue(v).profiles.keys.first()
            selectedAccount.set(Pair(v, profiles.authenticationDatabase.getValue(v)))
        }

    var selectedAccount = SimpleObjectProperty<Pair<String, LauncherProfileAuthenticationEntry>?>()

    init {
        reload()
        selectedAccount.addListener { _, _, newAccount ->
            selectedID = newAccount?.first
        }
    }

    fun reload() {
        profiles = LauncherProfiles.parse(DATA_DIR.join(LauncherProfiles.FILENAME))
        yggdrasil = Yggdrasil(profiles.clientToken)
        accounts = profiles.authenticationDatabase.asObservable()
        if (selectedID != null) {
            selectedAccount.set(Pair(selectedID!!, profiles.authenticationDatabase.getValue(selectedID!!)))
        }
    }

    fun interactiveLogin(stage: Stage, refresh: Boolean = false): Boolean {
        var view = tornadofx.find<AddAccount>()
        if (refresh && selectedID != null) {
            view = view.with(
                selectedAccount.get()!!.second.username,
                "Failed to verify the saved credentials, please login again."
            )
        }
        view.openModal(owner = stage, block = true)

        return if (!view.submitted) {
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
                    content = ex.message!!
                )
                false
            }
        }
    }

    fun login(username: String, password: String) {
        val (id, entry) = LauncherProfileAuthenticationEntry.fromYggdrasil(yggdrasil.authenticate(username, password))
        accounts[id] = entry

        if (accounts.size == 1 || selectedID == null) {
            selectedID = id
        }

        profiles.save(DATA_DIR.join(LauncherProfiles.FILENAME))
    }

    fun logout() {
        val selAcc = selectedAccount.get()
        if (selAcc != null) {
            val yggAcc = selAcc.second.toYggdrasil(selAcc.first)
            yggdrasil.invalidate(yggAcc)
        }

        if (selectedID == selAcc?.first) {
            selectedID = accounts.keys.first()
        }

        profiles.save(DATA_DIR.join(LauncherProfiles.FILENAME))
    }

    fun verifyAccount(): Boolean {
        val selAcc = selectedAccount.get() ?: return false
        val yggAcc = selAcc.second.toYggdrasil(selAcc.first)

        return if (yggdrasil.validate(yggAcc)) {
            true
        } else {
            try {
                val (id, entry) = LauncherProfileAuthenticationEntry.fromYggdrasil(yggdrasil.refresh(yggAcc))
                this.accounts[id] = entry
                true
            } catch (ex: ForbiddenException) {
                false
            }
        }
    }
}