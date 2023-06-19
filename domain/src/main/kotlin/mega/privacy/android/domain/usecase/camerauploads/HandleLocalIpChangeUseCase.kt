package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * [HandleLocalIpChangeUseCase] to initiate mega api for connection
 */
class HandleLocalIpChangeUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Initiate mega api if required based on IP
     */
    suspend operator fun invoke(shouldRetryChatConnections: Boolean) {
        val previousIP = environmentRepository.getIpAddress()
        val currentIP = environmentRepository.getLocalIpAddress()
        environmentRepository.setIpAddress(currentIP)
        if (!currentIP.isNullOrEmpty() && currentIP.compareTo(HOST_ADDRESS) != 0) {
            if (previousIP == null || currentIP.compareTo(previousIP) != 0) {
                accountRepository.reconnect()
                if (shouldRetryChatConnections) {
                    accountRepository.retryChatPendingConnections(disconnect = true)
                }
            } else {
                accountRepository.retryPendingConnections()
                if (shouldRetryChatConnections) {
                    accountRepository.retryChatPendingConnections(disconnect = false)
                }
            }
        }
    }

    private companion object {
        const val HOST_ADDRESS = "127.0.0.1"
    }
}
