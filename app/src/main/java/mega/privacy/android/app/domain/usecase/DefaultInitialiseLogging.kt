package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.logging.loggers.FileLogMessage
import mega.privacy.android.app.logging.loggers.FileLogger
import javax.inject.Inject

/**
 * Default initialise logging
 *
 * @property loggingRepository
 * @property areSdkLogsEnabled
 * @property areChatLogsEnabled
 * @property coroutineDispatcher
 */
class DefaultInitialiseLogging @Inject constructor(
    private val loggingRepository: LoggingRepository,
    private val areSdkLogsEnabled: AreSdkLogsEnabled,
    private val areChatLogsEnabled: AreChatLogsEnabled,
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher,
) : InitialiseLogging {
    override suspend fun invoke(isDebug: Boolean) {
        if (isDebug) loggingRepository.enableLogAllToConsole()

        coroutineScope {
            launch(coroutineDispatcher){ monitorSdkLoggingSetting() }
            launch(coroutineDispatcher){ monitorChatLoggingSetting() }
        }

    }

    private suspend fun monitorSdkLoggingSetting() {
        areSdkLogsEnabled()
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) loggingRepository.getSdkLoggingFlow() else emptyFlow()
            }.collect{
                loggingRepository.logToSdkFile(it)
            }
    }

    private suspend fun monitorChatLoggingSetting() {
        areChatLogsEnabled()
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) loggingRepository.getChatLoggingFlow() else emptyFlow()
            }.collect{
                loggingRepository.logToChatFile(it)
            }
    }
}