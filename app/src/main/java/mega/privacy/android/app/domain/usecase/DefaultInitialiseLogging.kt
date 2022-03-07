package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.repository.LoggingRepository
import javax.inject.Inject

/**
 * Default initialise logging
 *
 * @property loggingRepository
 * @property areSdkLogsEnabled
 * @property areChatLogsEnabled
 */
class DefaultInitialiseLogging @Inject constructor(
    private val loggingRepository: LoggingRepository,
    private val areSdkLogsEnabled: AreSdkLogsEnabled,
    private val areChatLogsEnabled: AreChatLogsEnabled,
) : InitialiseLogging {
    override suspend fun invoke(isDebug: Boolean) {
        if (isDebug) {
            loggingRepository.enableLogAllToConsole()
        } else {
            coroutineScope {
                launch{ monitorSdkLoggingSetting() }
                launch{ monitorChatLoggingSetting() }
            }
        }
    }

    private suspend fun monitorSdkLoggingSetting() {
        areSdkLogsEnabled()
            .distinctUntilChanged()
            .collect { enabled ->
            if (enabled) {
                loggingRepository.enableWriteSdkLogsToFile()
            } else {
                loggingRepository.disableWriteSdkLogsToFile()
            }
        }
    }

    private suspend fun monitorChatLoggingSetting() {
        areChatLogsEnabled()
            .distinctUntilChanged()
            .collect { enabled ->
            if (enabled) {
                loggingRepository.enableWriteChatLogsToFile()
            } else {
                loggingRepository.disableWriteChatLogsToFile()
            }
        }
    }
}