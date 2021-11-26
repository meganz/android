package mega.privacy.android.app.service.crashreporter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter

class CrashReporterImpl : CrashReporter {

    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    override fun report(e: Throwable) {
        crashlytics.recordException(e)
        crashlytics.sendUnsentReports()
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}