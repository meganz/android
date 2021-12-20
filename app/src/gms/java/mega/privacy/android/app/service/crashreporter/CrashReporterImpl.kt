package mega.privacy.android.app.service.crashreporter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter
import mega.privacy.android.app.utils.LogUtil

class CrashReporterImpl : CrashReporter {

    private var crashlytics: FirebaseCrashlytics? = null

    init {
        try {
            crashlytics = FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.logError(e.message)
        }
    }

    override fun report(e: Throwable) {
        crashlytics?.recordException(e)
        crashlytics?.sendUnsentReports()
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics?.setCrashlyticsCollectionEnabled(enabled)
    }
}