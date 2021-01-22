package mega.privacy.android.app.utils

import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer

object RxUtil {
    @JvmField
    val IGNORE = Action {}

    @JvmStatic
    fun logErr(context: String): Consumer<in Throwable> {
        return Consumer { throwable: Throwable? ->
            LogUtil.logError("$context onError", throwable)
        }
    }
}
