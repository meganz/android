package mega.privacy.android.app.logging.loggers

import android.util.Log
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatLoggerInterface
import timber.log.Timber

class ChatTimberLogger: MegaChatLoggerInterface {
    override fun log(loglevel: Int, message: String?) {
        Timber.log(getPriority(loglevel), message)
    }

    private fun getPriority(logLevel: Int): Int {
        return when (logLevel) {
            MegaApiAndroid.LOG_LEVEL_DEBUG -> Log.DEBUG
            MegaApiAndroid.LOG_LEVEL_ERROR,
            MegaApiAndroid.LOG_LEVEL_FATAL -> Log.ERROR
            MegaApiAndroid.LOG_LEVEL_INFO -> Log.INFO
            MegaApiAndroid.LOG_LEVEL_MAX -> Log.VERBOSE
            MegaApiAndroid.LOG_LEVEL_WARNING -> Log.WARN
            else -> Log.INFO
        }
    }
}