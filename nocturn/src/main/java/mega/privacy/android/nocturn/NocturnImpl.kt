package mega.privacy.android.nocturn

import mega.privacy.android.nocturn.notification.NocturnNotificator
import mega.privacy.android.nocturn.thread.AnrDetectorThread

class NocturnImpl(
    private val notificator: NocturnNotificator,
) : Nocturn {
    override fun monitor(waitTimeout: Long) {
        notificator.setup()
        startAnrDetector(waitTimeout)
    }

    private fun startAnrDetector(waitTimeout: Long) {
        AnrDetectorThread.spawn(
            waitTimeout = waitTimeout,
            onAnrDetected = ::handleAnr,
        )
    }

    private fun handleAnr(tag: String, stackTrace: Array<StackTraceElement>) {
        val summaries = stackTrace
            .filter { it.className.lowercase().contains("mega") }
            .map { "$it" }
            .ifEmpty { stackTrace.take(3).map { "$it" } }
        notificator.alertAnr(tag, summaries)
    }
}
