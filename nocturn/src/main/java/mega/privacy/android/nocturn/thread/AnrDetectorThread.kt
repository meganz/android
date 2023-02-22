package mega.privacy.android.nocturn.thread

import android.os.Debug
import android.os.Handler
import android.os.Looper
import mega.privacy.android.nocturn.exception.NocturnException

internal class AnrDetectorThread(
    private val waitTimeout: Long,
    private val onAnrDetected: (tag: String, stackTrace: Array<StackTraceElement>) -> Unit,
) : Thread("NocturnThread") {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val heartbeatRunnable: Runnable = Runnable(::ackHeartbeat)

    private val isDebuggingSession: Boolean
        get() = Debug.waitingForDebugger() || Debug.isDebuggerConnected()

    @Volatile
    private var waitTime: Long = 0L

    private var isAnrDetected: Boolean = false

    override fun run() {
        while (isAlive && !isInterrupted && !isDebuggingSession && !isAnrDetected) {
            waitTime += MONITOR_INTERVAL
            if (waitTime >= waitTimeout) handleAnr()

            sleep(MONITOR_INTERVAL)
            mainHandler.post(heartbeatRunnable)
        }
    }

    private fun ackHeartbeat() {
        waitTime = 0L
    }

    private fun handleAnr() {
        waitTime = 0L
        isAnrDetected = true

        val stackTrace = Looper.getMainLooper().thread.stackTrace
        val tag = "${stackTrace.hashCode()}"
        onAnrDetected(tag, stackTrace)

        spawn(waitTimeout, onAnrDetected)
        throw NocturnException("$tag (Timeout = ${waitTimeout}ms)").also { it.stackTrace = stackTrace }
    }

    companion object {
        private const val MONITOR_INTERVAL: Long = 100L

        fun spawn(
            waitTimeout: Long,
            onAnrDetected: (tag: String, stackTrace: Array<StackTraceElement>) -> Unit,
        ) = AnrDetectorThread(waitTimeout, onAnrDetected).start()
    }
}
