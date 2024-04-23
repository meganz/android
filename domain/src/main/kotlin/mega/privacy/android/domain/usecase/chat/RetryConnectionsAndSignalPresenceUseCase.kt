package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import javax.inject.Inject

/**
 * A use case to retry the pending connections and signal the presence to the SDK.
 *
 * @property chatRepository
 * @property retryPendingConnectionsUseCase
 */
class RetryConnectionsAndSignalPresenceUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val retryPendingConnectionsUseCase: RetryPendingConnectionsUseCase,
) {

    /**
     * Invocation method.
     *
     * @return True if successfully signal the presence to the SDK, false otherwise
     */
    suspend operator fun invoke(): Boolean {
        retryPendingConnectionsUseCase(disconnect = false)
        val chatPresenceConfig = chatRepository.getChatPresenceConfig()
        return if (chatPresenceConfig != null && !chatPresenceConfig.isPending) {
            chatRepository.signalPresenceActivity()
            true
        } else false
    }
}
