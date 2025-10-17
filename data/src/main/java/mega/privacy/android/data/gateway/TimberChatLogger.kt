package mega.privacy.android.data.gateway

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.Keep
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatLoggerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat file logger
 *
 * Chat log listener that prints all the chat output to file. See logback.xml for configuration
 */
@Keep
internal class TimberChatLogger @Inject constructor() : MegaChatLoggerInterface {
    @SuppressLint("LogNotTimber")
    @Synchronized
    override fun log(loglevel: Int, message: String?) {
        try{
            Timber.tag(TAG)
            when (loglevel) {
                MegaChatApi.LOG_LEVEL_MAX -> Timber.v(message)
                MegaChatApi.LOG_LEVEL_DEBUG -> Timber.d(message)
                MegaChatApi.LOG_LEVEL_INFO -> Timber.i(message)
                MegaChatApi.LOG_LEVEL_WARNING -> Timber.w(message)
                MegaChatApi.LOG_LEVEL_ERROR -> Timber.e(message)
                else -> Timber.i(message)
            }
        }catch (t: Throwable) {
            // swallow everything to protect native code
            Log.e(TAG, "Exception in SDK logger, swallowed to prevent SIGABRT", t)
        }
    }

    companion object {
        const val TAG = "[chat_sdk]"
    }
}