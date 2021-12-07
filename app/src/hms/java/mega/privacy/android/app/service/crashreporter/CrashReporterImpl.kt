package mega.privacy.android.app.service.crashreporter

import com.huawei.agconnect.crash.AGConnectCrash
import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter

class CrashReporterImpl : CrashReporter {

    private val agConnectCrash = AGConnectCrash.getInstance()

    override fun report(e: Throwable) {
        agConnectCrash.recordException(e)
    }

    override fun setEnabled(enabled: Boolean) {
        agConnectCrash.enableCrashCollection(enabled)
    }
}