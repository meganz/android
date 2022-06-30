package mega.privacy.android.app.presentation.logging.tree

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.data.gateway.TimberChatLogger
import mega.privacy.android.app.data.gateway.TimberMegaLogger
import mega.privacy.android.app.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.app.domain.entity.logging.LogEntry
import mega.privacy.android.app.domain.usecase.CreateLogEntry
import timber.log.Timber

/**
 * Sdk log flow tree
 *
 * Implementation of [Timber.Tree] that converts logging events from the sdk listener to a flow
 *
 * @property logFlow a flow where all sdk log messages are emitted
 */
class LogFlowTree(
    dispatcher: CoroutineDispatcher,
    private val createLogEntry: CreateLogEntry,
) : Timber.Tree() {

    private val scope = CoroutineScope(Job() + dispatcher)

    private val _logFlow =
        MutableSharedFlow<LogEntry>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val logFlow: SharedFlow<LogEntry> = _logFlow

    private val ignoredClasses = listOf(
        Timber::class.java.name,
        Timber::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        LogFlowTree::class.java.name,
    )

    private val sdkLoggers = listOf(
        TimberChatLogger::class.java.name,
        TimberMegaLogger::class.java.name
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val trace = Throwable().stackTrace
        scope.launch {
            createLogEntry(
                CreateLogEntryRequest(
                    tag = tag,
                    message = message,
                    priority = priority,
                    throwable = t,
                    trace = trace.asList(),
                    loggingClasses = ignoredClasses,
                    sdkLoggers = sdkLoggers)
            )?.let { _logFlow.emit(it) }
        }
    }

}