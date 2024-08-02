package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Default initialise logging
 *
 * @property loggingRepository
 * @property coroutineDispatcher
 */
class InitialiseLoggingUseCase @Inject constructor(
    private val loggingRepository: LoggingRepository,
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke() {
        coroutineScope {
            launch(coroutineDispatcher) { monitorSdkLoggingSetting() }
            launch(coroutineDispatcher) { monitorChatLoggingSetting() }
        }
    }

    private suspend fun monitorSdkLoggingSetting() {
        loggingRepository.getSdkLoggingFlow()
            .collect {
                loggingRepository.logToSdkFile(it)
            }
    }

    private suspend fun monitorChatLoggingSetting() {
        loggingRepository.getChatLoggingFlow()
            .collect {
                loggingRepository.logToChatFile(it)
            }
    }
}