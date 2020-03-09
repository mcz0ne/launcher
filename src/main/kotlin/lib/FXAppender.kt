package lib

import ch.qos.logback.core.OutputStreamAppender
import tornadofx.FX
import tornadofx.FXEvent
import java.io.OutputStream

class FXAppender<E> : OutputStreamAppender<E>() {
    class Line(val line: String) : FXEvent()

    class FXOutputStream : OutputStream() {
        private var buff = StringBuilder()

        override fun write(b: Int) {
            buff.appendCodePoint(b)

            if (FX.getPrimaryStage() != null && buff.contains("\n")) {
                FX.eventbus.fire(Line(buff.substring(0, buff.lastIndexOf("\n") + 1)))
                buff.delete(0, buff.lastIndexOf("\n") + 1)
            }
        }

    }

    private var buffer = FXOutputStream()

    override fun start() {
        outputStream = buffer
        super.start()
    }

}