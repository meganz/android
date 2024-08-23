package mega.privacy.android.data.gateway

import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatLoggerInterface
import timber.log.Timber
import javax.inject.Inject
import androidx.annotation.Keep

/**
 * Chat file logger
 *
 * Chat log listener that prints all the chat output to file. See logback.xml for configuration
 */
@Keep
internal class TimberChatLogger @Inject constructor() : MegaChatLoggerInterface {
    @Synchronized
    override fun log(loglevel: Int, message: String?) {
        when (loglevel) {
            MegaChatApi.LOG_LEVEL_MAX, MegaChatApi.LOG_LEVEL_VERBOSE -> Timber.v(message)
            MegaChatApi.LOG_LEVEL_DEBUG -> Timber.d(message)
            MegaChatApi.LOG_LEVEL_INFO -> Timber.i(message)
            MegaChatApi.LOG_LEVEL_WARNING -> Timber.w(message)
            MegaChatApi.LOG_LEVEL_ERROR -> Timber.e(message)
            else -> Timber.i(message)
        }
    }
}