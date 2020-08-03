package mega.privacy.android.app.utils

import io.reactivex.rxjava3.functions.Consumer

object RxUtil {
    fun logErr(context: String): Consumer<in Throwable> {
        return Consumer { throwable: Throwable? ->
            LogUtil.logError("$context onError", throwable)
        }
    }
}
