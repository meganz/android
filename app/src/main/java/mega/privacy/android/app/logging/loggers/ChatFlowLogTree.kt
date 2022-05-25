package mega.privacy.android.app.logging.loggers

import android.text.format.DateFormat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import mega.privacy.android.app.logging.TimberLegacyLog
import mega.privacy.android.app.utils.LogUtil
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Chat flow log tree
 *
 * Implementation of [Timber.Tree] that converts logging events from the chat listener to a flow
 *
 * @property logFlow a flow where all chat sdk log messages are emitted
 */
class ChatFlowLogTree @Inject constructor(
) : Timber.Tree() {

    private val ignoredClasses = listOf(
            Timber::class.java.name,
            Timber.Forest::class.java.name,
            Timber.Tree::class.java.name,
            Timber.DebugTree::class.java.name,
            TimberLegacyLog::class.java.name,
            LogUtil::class.java.name,
            ChatFlowLogTree::class.java.name,
    )

    private val _logFlow =
            MutableSharedFlow<FileLogMessage>(
                    replay = 0,
                    extraBufferCapacity = 1,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    val logFlow: SharedFlow<FileLogMessage> = _logFlow

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isChatLog(tag)) {
            val trace = Throwable().stackTrace
            var megaTag: String? = null
            var stackTrace: String? = null

            if (isAppLog(trace)) {
                megaTag = createTag()
                stackTrace = createTrace(trace)
            }

            _logFlow.tryEmit(FileLogMessage(megaTag, message, stackTrace, priority, t))
        }
    }

    private fun isChatLog(tag: String?) = tag == null

    private fun isAppLog(trace: Array<out StackTraceElement?>?) =
            trace?.none { it?.className?.contains(TimberChatLogger::class.java.name) ?: true }
                    ?: true

    private fun createTrace(trace: Array<StackTraceElement>): String? {
        return trace
                .firstOrNull { it.className !in ignoredClasses }
                ?.let { "${it.fileName}#${it.methodName}:${it.lineNumber}" }
    }

    private fun createTag(): String = "[ ${getFormattedTime()} ][ clientApp ]"

    private fun getFormattedTime() = DateFormat.format("dd-MM HH:mm:ss", Calendar.getInstance(TimeZone.getTimeZone("UTC")))
}