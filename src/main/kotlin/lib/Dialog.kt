package lib

import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.stage.Window


object Dialog {
    inline fun alert(
        type: Alert.AlertType,
        header: String,
        content: String? = null,
        vararg buttons: ButtonType,
        owner: Window? = null,
        title: String? = null,
        actionFn: Alert.(ButtonType) -> Unit = {}
    ): Alert {
        val alert = Alert(type, content ?: "", *buttons)
        title?.let { alert.title = it }
        alert.headerText = header
        owner?.also { alert.initOwner(it) }

        alert.isResizable = true
        alert.dialogPane.minHeight = Region.USE_PREF_SIZE

        val buttonClicked = alert.showAndWait()
        if (buttonClicked.isPresent) {
            alert.actionFn(buttonClicked.get())
        }
        return alert
    }

    inline fun warning(
        header: String,
        content: String? = null,
        vararg buttons: ButtonType,
        owner: Window? = null,
        title: String? = null,
        actionFn: Alert.(ButtonType) -> Unit = {}
    ) = alert(Alert.AlertType.WARNING, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

    inline fun error(
        header: String,
        content: String? = null,
        vararg buttons: ButtonType,
        owner: Window? = null,
        title: String? = null,
        actionFn: Alert.(ButtonType) -> Unit = {}
    ) = alert(Alert.AlertType.ERROR, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

    inline fun information(
        header: String,
        content: String? = null,
        vararg buttons: ButtonType,
        owner: Window? = null,
        title: String? = null,
        actionFn: Alert.(ButtonType) -> Unit = {}
    ) = alert(Alert.AlertType.INFORMATION, header, content, *buttons, owner = owner, title = title, actionFn = actionFn)

    inline fun confirmation(
        header: String,
        content: String? = null,
        vararg buttons: ButtonType,
        owner: Window? = null,
        title: String? = null,
        actionFn: Alert.(ButtonType) -> Unit = {}
    ) = alert(
        Alert.AlertType.CONFIRMATION,
        header,
        content,
        *buttons,
        owner = owner,
        title = title,
        actionFn = actionFn
    )
}