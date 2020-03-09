package event

import lib.modpack.Modpack
import tornadofx.EventBus
import tornadofx.FXEvent

class ModpackUpdateEvent(val modpack: Modpack) : FXEvent(EventBus.RunOn.ApplicationThread)