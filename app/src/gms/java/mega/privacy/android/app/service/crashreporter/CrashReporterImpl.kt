package mega.privacy.android.app.service.crashreporter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter

class CrashReporterImpl: CrashReporter {

    override fun report(e: Throwable) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.recordException(e)
        crashlytics.sendUnsentReports()
    }
}