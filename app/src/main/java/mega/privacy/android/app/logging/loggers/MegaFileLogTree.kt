package mega.privacy.android.app.logging.loggers

import android.util.Log
import org.slf4j.LoggerFactory
import timber.log.Timber
import javax.inject.Inject

class MegaFileLogTree @Inject constructor() : Timber.Tree() {
    private val fileLogger = LoggerFactory.getLogger(TimberMegaLogger::class.java)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val logMessage = "${tag.orEmpty()} $message".trim()
        when (priority) {
            Log.VERBOSE -> fileLogger.trace(logMessage)
            Log.DEBUG -> fileLogger.debug(logMessage)
            Log.INFO -> fileLogger.info(logMessage)
            Log.ASSERT -> fileLogger.info(logMessage)
            Log.WARN -> fileLogger.warn(logMessage)
            Log.ERROR -> {
                if (t != null) {
                    fileLogger.error(logMessage, t)
                } else {
                    fileLogger.error(logMessage)
                }
            }
        }
    }
}