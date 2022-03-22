package mega.privacy.android.app.logging.loggers

import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatLoggerInterface
import org.slf4j.LoggerFactory
import javax.inject.Inject

class ChatFileLogger @Inject constructor() : MegaChatLoggerInterface {
    private val fileLogger = LoggerFactory.getLogger(ChatFileLogger::class.java)
    var captureLogs = false
    override fun log(loglevel: Int, message: String?) {
        if (captureLogs) {
            when (loglevel) {
                MegaApiAndroid.LOG_LEVEL_MAX -> fileLogger.trace(message)
                MegaApiAndroid.LOG_LEVEL_DEBUG -> fileLogger.debug(message)
                MegaApiAndroid.LOG_LEVEL_INFO -> fileLogger.info(message)
                MegaApiAndroid.LOG_LEVEL_WARNING -> fileLogger.warn(message)
                MegaApiAndroid.LOG_LEVEL_ERROR,
                MegaApiAndroid.LOG_LEVEL_FATAL -> fileLogger.error(message)
            }
        }
    }
}