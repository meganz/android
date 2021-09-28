package mega.privacy.android.app.utils

import android.os.Handler
import android.os.Looper

object RunOnUIThreadUtils {

    private val handler = Handler(Looper.getMainLooper())

    fun runDelay(delayMs: Long, uiTask: () -> Unit) {
        handler.postDelayed(uiTask, delayMs)
    }

    fun post(uiTask: () -> Unit) {
        handler.post(uiTask)
    }

    fun stop(){
        handler.removeCallbacksAndMessages(null);
    }
}
