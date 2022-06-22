package mega.privacy.android.app.logging.loggers

import android.text.format.DateFormat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import mega.privacy.android.app.logging.TimberLegacyLog
import mega.privacy.android.app.utils.LogUtil
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Sdk log flow tree
 *
 * Implementation of [Timber.Tree] that converts logging events from the sdk listener to a flow
 *
 * @property logFlow a flow where all sdk log messages are emitted
 */
class SdkLogFlowTree @Inject constructor(
) : Timber.Tree() {

    private val _logFlow =
        MutableSharedFlow<FileLogMessage>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val logFlow: SharedFlow<FileLogMessage> = _logFlow

    private val ignoredClasses = listOf(
        Timber::class.java.name,
        Timber.Forest::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        TimberLegacyLog::class.java.name,
        LogUtil::class.java.name,
        SdkLogFlowTree::class.java.name,
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isSdkLog(tag)) {
            _logFlow.tryEmit(FileLogMessage(tag, message, null, priority, t))
        } else {
            val trace = Throwable().stackTrace
            if (isAppLog(trace)) {
                _logFlow.tryEmit(
                    FileLogMessage(
                        createTag(),
                        message,
                        createTrace(trace),
                        priority,
                        t
                    )
                )
            }
        }
    }

    private fun isSdkLog(tag: String?) = tag != null

    private fun isAppLog(trace: Array<out StackTraceElement?>?) =
        trace?.none { it?.className?.contains(TimberChatLogger::class.java.name) ?: true }
            ?: true

    private fun createTrace(trace: Array<StackTraceElement>): String? {
        return trace
            .firstOrNull { it.className !in ignoredClasses }
            ?.let { "${it.fileName}#${it.methodName}:${it.lineNumber}" }
    }

    private fun createTag(): String = "[${getFormattedTime()}][clientApp]"

    private fun getFormattedTime() =
        DateFormat.format("dd-MM HH:mm:ss", Calendar.getInstance(TimeZone.getTimeZone("UTC")))
}