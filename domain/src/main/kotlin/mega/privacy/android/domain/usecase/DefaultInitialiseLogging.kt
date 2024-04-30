package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.logging.AreChatLogsEnabledUseCase
import mega.privacy.android.domain.usecase.logging.AreSdkLogsEnabledUseCase
import javax.inject.Inject

/**
 * Default initialise logging
 *
 * @property loggingRepository
 * @property areSdkLogsEnabledUseCase
 * @property areChatLogsEnabledUseCase
 * @property coroutineDispatcher
 */
internal class DefaultInitialiseLogging @Inject constructor(
    private val loggingRepository: LoggingRepository,
    private val areSdkLogsEnabledUseCase: AreSdkLogsEnabledUseCase,
    private val areChatLogsEnabledUseCase: AreChatLogsEnabledUseCase,
    private val coroutineDispatcher: CoroutineDispatcher,
) : InitialiseLogging {

    override suspend fun invoke(overrideEnabledSettings: Boolean) {
        coroutineScope {
            launch(coroutineDispatcher) { monitorSdkLoggingSetting(overrideEnabledSettings) }
            launch(coroutineDispatcher) { monitorChatLoggingSetting(overrideEnabledSettings) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun monitorSdkLoggingSetting(overrideEnabledSettings: Boolean) {
        areSdkLogsEnabledUseCase()
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled || overrideEnabledSettings) loggingRepository.getSdkLoggingFlow() else emptyFlow()
            }.collect {
                loggingRepository.logToSdkFile(it)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun monitorChatLoggingSetting(overrideEnabledSettings: Boolean) {
        areChatLogsEnabledUseCase()
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled || overrideEnabledSettings) loggingRepository.getChatLoggingFlow() else emptyFlow()
            }.collect {
                loggingRepository.logToChatFile(it)
            }
    }
}