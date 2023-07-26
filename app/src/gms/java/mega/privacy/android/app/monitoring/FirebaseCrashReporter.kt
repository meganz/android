package mega.privacy.android.app.monitoring

import com.google.firebase.crashlytics.FirebaseCrashlytics
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.monitoring.CrashReporter

/**
 * Report logs to FirebaseCrashlytics
 */
class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics
) : CrashReporter {

    override fun report(e: Throwable) {
        crashlytics.recordException(e)
        crashlytics.sendUnsentReports()
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled && !BuildConfig.DEBUG)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }
}
