package mega.privacy.android.app.service.crashreporter

import com.huawei.agconnect.crash.AGConnectCrash
import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter
import mega.privacy.android.app.utils.LogUtil

class CrashReporterImpl : CrashReporter {

    private var agConnectCrash: AGConnectCrash? = null

    init {
        try {
            agConnectCrash = AGConnectCrash.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.logError(e.message)
        }
    }

    override fun report(e: Throwable) {
        agConnectCrash?.recordException(e)
    }

    override fun setEnabled(enabled: Boolean) {
        agConnectCrash?.enableCrashCollection(enabled)
    }
}