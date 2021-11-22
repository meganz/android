package mega.privacy.android.app.service.crashreporter

import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter

class CrashReporterImpl : CrashReporter {

    override fun report(e: Throwable) {
        // Haven't implemented for HMS.
    }

    override fun setEnabled(enabled: Boolean) {
        // Haven't implemented for HMS.
    }
}