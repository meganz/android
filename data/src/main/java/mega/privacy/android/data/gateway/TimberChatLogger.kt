package mega.privacy.android.data.gateway

import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatLoggerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat file logger
 *
 * Chat log listener that prints all the chat output to file. See logback.xml for configuration
 */
class TimberChatLogger @Inject constructor() : MegaChatLoggerInterface {
    override fun log(loglevel: Int, message: String?) {
        when (loglevel) {
            MegaApiAndroid.LOG_LEVEL_MAX -> Timber.v(message)
            MegaApiAndroid.LOG_LEVEL_DEBUG -> Timber.d(message)
            MegaApiAndroid.LOG_LEVEL_INFO -> Timber.i(message)
            MegaApiAndroid.LOG_LEVEL_WARNING -> Timber.w(message)
            MegaApiAndroid.LOG_LEVEL_ERROR,
            MegaApiAndroid.LOG_LEVEL_FATAL,
            -> Timber.e(message)
        }
    }
}