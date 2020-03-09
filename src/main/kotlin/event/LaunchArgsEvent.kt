package event

import tornadofx.FXEvent

class LaunchArgsEvent(
    val args: List<String>
): FXEvent()