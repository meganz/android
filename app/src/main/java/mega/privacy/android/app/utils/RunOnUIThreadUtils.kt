package mega.privacy.android.app.utils

import android.os.Handler


object RunOnUIThreadUtils {

    fun runDelay(delayMs: Long, uiTask: () -> Unit) {
        Handler().postDelayed(uiTask, delayMs)
    }

    fun post(uiTask: () -> Unit) {
        Handler().post(uiTask)
    }
}