package mega.privacy.android.data.datastore

import mega.privacy.android.domain.monitoring.CrashReporter

internal class RecordingCrashReporter : CrashReporter {
    val reported = mutableListOf<Throwable>()

    override fun report(e: Throwable) {
        reported += e
    }

    override fun setEnabled(enabled: Boolean) = Unit

    override fun log(message: String) = Unit
}
