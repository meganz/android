package mega.privacy.android.app.service.reporter

import com.huawei.agconnect.crash.AGConnectCrash
import mega.privacy.android.app.middlelayer.reporter.CrashReporter

class AppGalleryCrashReporter(
    private var agConnectCrash: AGConnectCrash
) : CrashReporter {

    override fun report(e: Throwable) {
        agConnectCrash.recordException(e)
    }

    override fun setEnabled(enabled: Boolean) {
        agConnectCrash.enableCrashCollection(enabled)
    }

    override fun log(message: String) {
    }
}
