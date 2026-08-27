package mega.privacy.android.data.gateway

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.core.Appender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mega.privacy.android.data.logging.FlushableRollingFileAppender
import mega.privacy.android.data.logging.LogbackContextProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
internal class LogbackLogFlushGateway @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val contextProvider: LogbackContextProvider,
) : LogFlushGateway {

    /**
     * Best-effort flush. Switches to the IO dispatcher so callers can invoke
     * it from any context (including main-thread lifecycle callbacks) without
     * blocking. Any non-cancellation failure is swallowed — crashing the app
     * while shutting down the log pipeline would be worse than losing a few
     * buffered log lines.
     *
     * Does nothing until logback has been configured: a lifecycle callback can
     * fire while [mega.privacy.android.data.logging.LogbackContextProvider] is
     * still configuring, and asking SLF4J for the factory here would hand that
     * configuring thread a substitute factory.
     */
    override suspend fun flush() = withContext(ioDispatcher) {
        swallow {
            contextProvider.ifConfigured { loggerContext ->
                loggerContext.loggerList.forEach { logger ->
                    swallow {
                        val iterator = logger.iteratorForAppenders()
                        while (iterator.hasNext()) {
                            flushAppender(iterator.next())
                        }
                    }
                }
            }
        }
    }

    private suspend fun flushAppender(appender: Appender<*>) {
        swallow {
            when (appender) {
                is FlushableRollingFileAppender<*> -> appender.flushNow()
                is AsyncAppender -> {
                    waitForQueueToDrain(appender)
                    val nested = appender.iteratorForAppenders()
                    while (nested.hasNext()) {
                        flushAppender(nested.next())
                    }
                }
            }
        }
    }

    /**
     * Give the [AsyncAppender] worker a brief window to drain its in-memory
     * queue into the underlying file appender before we flush the OutputStream
     * buffer. Without this wait, queued events would still be sitting in
     * memory when [FlushableRollingFileAppender.flushNow] runs and would be
     * lost if the process is killed right after.
     */
    private suspend fun waitForQueueToDrain(appender: AsyncAppender) {
        if (appender.numberOfElementsInQueue > 0) {
            delay(DRAIN_WAIT_MS)
        }
    }

    /**
     * [CancellationException] must be re-thrown so structured concurrency
     * keeps working — if the calling scope is cancelled, we want this flush
     * to abort too. Any other failure is best-effort: we'd rather lose a few
     * buffered log lines than crash the app on shutdown.
     */
    private inline fun swallow(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // best-effort
        }
    }

    private companion object {
        const val DRAIN_WAIT_MS = 100L
    }
}
