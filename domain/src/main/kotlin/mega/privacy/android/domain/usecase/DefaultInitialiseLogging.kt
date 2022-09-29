package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import mega.privacy.android.domain.repository.LoggingRepository
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
    private val startupLogging: StartupLogging,
    private val coroutineDispatcher: CoroutineDispatcher,
) : InitialiseLogging {

    override suspend fun invoke(isDebug: Boolean) {
        if (isDebug) {
            loggingRepository.enableLogAllToConsole()
        }
        startupLogging()

        coroutineScope {
            launch(coroutineDispatcher) { monitorSdkLoggingSetting() }
            launch(coroutineDispatcher) { monitorChatLoggingSetting() }
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun monitorSdkLoggingSetting() {
        areSdkLogsEnabled()
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) loggingRepository.getSdkLoggingFlow() else emptyFlow()
            }.collect {
                loggingRepository.logToSdkFile(it)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun monitorChatLoggingSetting() {
        areChatLogsEnabled()
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) loggingRepository.getChatLoggingFlow() else emptyFlow()
            }.collect {
                loggingRepository.logToChatFile(it)
            }
    }
}