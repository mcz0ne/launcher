package event

import tornadofx.EventBus
import tornadofx.FXEvent
import java.io.File
import java.net.URL

class UpdateEvent(
    val name: String,
    val definition: URL,
    val folder: File
) : FXEvent(EventBus.RunOn.BackgroundThread)