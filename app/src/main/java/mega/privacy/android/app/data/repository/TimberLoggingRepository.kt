package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
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
 * @property sdkLogFlowTree
 * @property chatLogFlowTree
 * @property loggingConfig
 * @property sdkLogger
 * @property chatLogger
 */
class TimberLoggingRepository @Inject constructor(
    private val timberMegaLogger: TimberMegaLogger,
    private val timberChatLogger: TimberChatLogger,
    private val sdkLogFlowTree: SdkLogFlowTree,
    private val chatLogFlowTree: SdkLogFlowTree,
    private val loggingConfig: LoggingConfigurationFacade,
    @SdkLogger private val sdkLogger: FileLogger,
    @ChatLogger private val chatLogger: FileLogger,
) : LoggingRepository {

    init {
        if (!Timber.forest().contains(sdkLogFlowTree)) {
            Timber.plant(sdkLogFlowTree)
        }
        if (!Timber.forest().contains(chatLogFlowTree)) {
            Timber.plant(chatLogFlowTree)
        }
        MegaChatApiAndroid.setLoggerObject(timberChatLogger)
        MegaApiAndroid.addLoggerObject(timberMegaLogger)
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

    override fun getSdkLoggingFlow(): Flow<FileLogMessage> = sdkLogFlowTree.logFlow.onSubscription {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        loggingConfig.resetLoggingConfiguration()
    }.onCompletion {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
    }

    override fun getChatLoggingFlow(): Flow<FileLogMessage> =
        chatLogFlowTree.logFlow.onSubscription {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
            loggingConfig.resetLoggingConfiguration()
        }.onCompletion {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
        }

    override fun logToSdkFile(logMessage: FileLogMessage) = sdkLogger.logToFile(logMessage)

    override fun logToChatFile(logMessage: FileLogMessage) = chatLogger.logToFile(logMessage)
}