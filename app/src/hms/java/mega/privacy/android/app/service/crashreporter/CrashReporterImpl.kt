package mega.privacy.android.app.service.crashreporter

import com.huawei.agconnect.crash.AGConnectCrash
import mega.privacy.android.app.middlelayer.crashreporter.CrashReporter

class CrashReporterImpl : CrashReporter {

    override fun report(e: Throwable) {
        AGConnectCrash.getInstance().recordException(e)
    }
}