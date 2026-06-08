package mega.privacy.android.data.logging

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mega.privacy.android.data.gateway.LogWriterGateway
import mega.privacy.android.data.gateway.TimberChatLogger
import mega.privacy.android.data.gateway.TimberMegaLogger
import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogPriority
import mega.privacy.android.domain.usecase.CreateLogEntry
import timber.log.Timber

/**
 * Timber tree that hands every log line to a single background worker which
 * builds the [mega.privacy.android.domain.entity.logging.LogEntry] and writes
 * it through [LogWriterGateway] (SLF4J → Logback).
 *
 * Replaces the previous flow-based pipeline (Channel → SharedFlow → external
 * collector → withContext-per-entry) with a single channel + single worker
 * thread per tree. Caller threads only pay the cost of [Channel.trySend].
 */
internal class LogFileTree(
    dispatcher: CoroutineDispatcher,
    private val createLogEntry: CreateLogEntry,
    private val writer: LogWriterGateway,
) : Timber.Tree() {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher.limitedParallelism(1))

    private val ignoredClasses = listOf(
        Timber::class.java.name,
        Timber.Forest::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        LogFileTree::class.java.name,
    )

    private val sdkLoggers = listOf(
        TimberChatLogger::class.java.name,
        TimberMegaLogger::class.java.name,
    )

    private val channel = Channel<CreateLogEntryRequest>(
        capacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch {
            for (request in channel) {
                createLogEntry(request)?.let(writer::writeLogEntry)
            }
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Skip the caller stack capture for high-volume SDK/Chat logs (they
        // already carry their own [sdk]/[chat_sdk] tag). For client-app logs,
        // keep the original behaviour so the log file still shows the
        // "file#method:line" prefix that aids debugging.
        val trace = if (tag == TimberChatLogger.TAG || tag == TimberMegaLogger.TAG) {
            emptyList()
        } else {
            Throwable().stackTrace.take(10)
        }
        channel.trySend(
            CreateLogEntryRequest(
                tag = tag,
                message = message,
                priority = LogPriority.fromInt(priority),
                throwable = t,
                trace = trace,
                loggingClasses = ignoredClasses,
                sdkLoggers = sdkLoggers,
            )
        )
    }
}
