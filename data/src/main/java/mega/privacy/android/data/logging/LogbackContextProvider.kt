package mega.privacy.android.data.logging

import androidx.annotation.VisibleForTesting
import ch.qos.logback.classic.LoggerContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.ILoggerFactory
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of the logback [LoggerContext].
 *
 * SLF4J bootstraps its provider lazily, exactly once, on whichever thread
 * touches [LoggerFactory] first, and hands every other thread a
 * [org.slf4j.helpers.SubstituteLoggerFactory] for as long as that bootstrap
 * runs — those threads are not blocked, so what they get back cannot be cast to
 * [LoggerContext]. Routing every access through here keeps the bootstrap on a
 * single thread: configuration runs first while holding the lock, and no other
 * caller touches [LoggerFactory] before it has finished.
 */
@Singleton
internal class LogbackContextProvider @VisibleForTesting internal constructor(
    private val loggerFactory: () -> ILoggerFactory,
) {

    @Inject
    constructor() : this({ LoggerFactory.getILoggerFactory() })

    private val mutex = Mutex()

    @Volatile
    private var configured = false

    /**
     * Applies [block] to the logback context and opens the gate for
     * [ifConfigured] once [block] reports that the configuration took effect.
     *
     * Returns false when logback is not the bound SLF4J provider, or when
     * [block] could not apply its configuration.
     */
    suspend fun configure(block: suspend (LoggerContext) -> Boolean): Boolean = mutex.withLock {
        val loggerContext = loggerContext() ?: return@withLock false
        block(loggerContext).also { if (it) configured = true }
    }

    /**
     * Runs [block] only once [configure] has succeeded. Before that there are no
     * appenders to act on anyway, and touching [LoggerFactory] here would start
     * the SLF4J bootstrap on this thread and leave the configuring thread with a
     * substitute factory.
     */
    suspend fun ifConfigured(block: suspend (LoggerContext) -> Unit) {
        if (!configured) return
        mutex.withLock { loggerContext()?.let { block(it) } }
    }

    private fun loggerContext() = loggerFactory() as? LoggerContext
}
