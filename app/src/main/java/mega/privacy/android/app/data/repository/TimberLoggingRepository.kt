package mega.privacy.android.app.data.repository

import mega.privacy.android.app.data.facade.LoggingConfigurationFacade
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.logging.loggers.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject

class TimberLoggingRepository @Inject constructor(
    private val timberMegaLogger: TimberMegaLogger,
    private val chatFileLogger: ChatFileLogger,
    private val sdkLoggingTree: MegaFileLogTree,
    private val loggingConfig: LoggingConfigurationFacade
) : LoggingRepository {

    init {
        MegaChatApiAndroid.setLoggerObject(chatFileLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
    }

    override fun enableWriteSdkLogsToFile() {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        if (!Timber.forest().contains(sdkLoggingTree)) {
            Timber.plant(sdkLoggingTree)
        }
        loggingConfig.resetLoggingConfiguration()
    }

    override fun disableWriteSdkLogsToFile() {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
        if (Timber.forest().contains(sdkLoggingTree)) {
            Timber.uproot(sdkLoggingTree)
        }
    }

    override fun enableWriteChatLogsToFile() {
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        chatFileLogger.captureLogs = true
        loggingConfig.resetLoggingConfiguration()
    }

    override fun disableWriteChatLogsToFile() {
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
        chatFileLogger.captureLogs = false
    }

    override fun enableLogAllToConsole() {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setLoggerObject(ChatTimberLogger())
        Timber.plant(LineNumberDebugTree())
    }

    override fun resetSdkLogging() {
        MegaApiAndroid.removeLoggerObject(timberMegaLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
    }

}