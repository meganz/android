package mega.privacy.android.app.data.repository

import mega.privacy.android.app.data.facade.LoggingConfigurationFacade
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.logging.loggers.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * [LoggingRepository] implementation
 *
 * @property timberMegaLogger
 * @property timberChatLogger
 * @property fileLogTree
 * @property loggingConfig
 * @property sdkLogger
 * @property chatLogger
 * @constructor Create empty Timber logging repository
 */
class TimberLoggingRepository @Inject constructor(
    private val timberMegaLogger: TimberMegaLogger,
    private val timberChatLogger: TimberChatLogger,
    private val fileLogTree: MegaFileLogTree,
    private val loggingConfig: LoggingConfigurationFacade,
    @SdkLogger private val sdkLogger: FileLogger,
    @ChatLogger private val chatLogger: FileLogger,
) : LoggingRepository {

    init {
        if (!Timber.forest().contains(fileLogTree)) {
            Timber.plant(fileLogTree)
        }
        MegaChatApiAndroid.setLoggerObject(timberChatLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
    }

    override fun enableWriteSdkLogsToFile() {
        sdkLogger.enabled = true
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        loggingConfig.resetLoggingConfiguration()
    }

    override fun disableWriteSdkLogsToFile() {
        sdkLogger.enabled = false
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
    }

    override fun enableWriteChatLogsToFile() {
        chatLogger.enabled = true
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        loggingConfig.resetLoggingConfiguration()
    }

    override fun disableWriteChatLogsToFile() {
        chatLogger.enabled = false
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
    }

    override fun enableLogAllToConsole() {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        Timber.plant(LineNumberDebugTree())
    }

    override fun resetSdkLogging() {
        MegaApiAndroid.removeLoggerObject(timberMegaLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
    }

}