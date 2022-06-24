package mega.privacy.android.app.logging.loggers

import android.util.Log
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaLoggerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Timber mega logger
 *
 * Class responsible for writing SDK log output to the Timber log, adding custom tags.
 *
 * See logback.xml for configuration.
 */
class TimberMegaLogger @Inject constructor() : MegaLoggerInterface {
    override fun log(time: String?, loglevel: Int, source: String?, message: String?) {
        Timber.tag("[$time][sdk][ ${getLogLevelString(loglevel)} ]")
        Timber.log(
            priority = getPriority(loglevel),
            message = "$message ${getSource(source)}",
        )
    }

    private fun getPriority(logLevel: Int): Int {
        return when (logLevel) {
            MegaApiAndroid.LOG_LEVEL_DEBUG -> Log.DEBUG
            MegaApiAndroid.LOG_LEVEL_ERROR -> Log.ERROR
            MegaApiAndroid.LOG_LEVEL_FATAL -> Log.ERROR
            MegaApiAndroid.LOG_LEVEL_INFO -> Log.INFO
            MegaApiAndroid.LOG_LEVEL_MAX -> Log.VERBOSE
            MegaApiAndroid.LOG_LEVEL_WARNING -> Log.WARN
            else -> Log.INFO
        }
    }

    private fun getLogLevelString(logLevel: Int): String {
        return when (logLevel) {
            MegaApiAndroid.LOG_LEVEL_DEBUG -> "DEB"
            MegaApiAndroid.LOG_LEVEL_ERROR -> "ERR"
            MegaApiAndroid.LOG_LEVEL_FATAL -> "FAT"
            MegaApiAndroid.LOG_LEVEL_INFO -> "INF"
            MegaApiAndroid.LOG_LEVEL_MAX -> "MAX"
            MegaApiAndroid.LOG_LEVEL_WARNING -> "WRN"
            else -> "NON"
        }
    }

    private fun getSource(source: String?): String? {
        return source?.split("jni/mega")?.getOrNull(1) ?: source
    }
}