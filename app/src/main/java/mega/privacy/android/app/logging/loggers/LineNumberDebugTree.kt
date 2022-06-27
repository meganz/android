package mega.privacy.android.app.logging.loggers

import android.util.Log
import timber.log.Timber

/**
 * Line number debug tree
 *
 * Debug log output tree for logcat
 */
class LineNumberDebugTree : Timber.DebugTree() {
    private val ignoredClasses = listOf(
        Timber::class.java.name,
        Timber.Forest::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        SdkLogFlowTree::class.java.name,
        ChatFlowLogTree::class.java.name,
        LineNumberDebugTree::class.java.name,
        TimberMegaLogger::class.java.name,
        TimberChatLogger::class.java.name,
    )

    private fun createTag(): String? {
        return Throwable().stackTrace
            .firstOrNull { filterKnownLoggerClasses(it) && filterDelegateLoggerClasses(it) }
            ?.let { "(${it.fileName}:${it.lineNumber})#${it.methodName}" }
    }

    private fun filterKnownLoggerClasses(it: StackTraceElement) =
        it.className !in ignoredClasses

    private fun filterDelegateLoggerClasses(it: StackTraceElement) =
        !it.className.contains("logger", true)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, "${prioritySymbol(priority)} ${createTag()}", message, t)
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

