package mega.privacy.android.app.logging.loggers

import android.util.Log
import timber.log.Timber

class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, "${prioritySymbol(priority)} $tag", message, t)
    }

    private fun prioritySymbol(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "\uD83D\uDC9C"
            Log.DEBUG -> "\uD83D\uDC99"
            Log.INFO -> "\uD83D\uDC9A"
            Log.ASSERT -> "\uD83D\uDC9B"
            Log.WARN -> "\uD83D\uDC9B"
            Log.ERROR -> "\uD83D\uDC94"
            else -> "???"
        }
    }
}
