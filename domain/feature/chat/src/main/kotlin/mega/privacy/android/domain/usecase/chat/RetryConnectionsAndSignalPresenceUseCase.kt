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
     * @param needSignalPresence Whether to signal presence activity when presenceConfig is available.
     *                           Pass false to skip signaling (e.g. in MeetingActivity).
     * @return True if presenceConfig is available and not pending, false otherwise
     */
    suspend operator fun invoke(needSignalPresence: Boolean = true): Boolean {
        retryPendingConnectionsUseCase(disconnect = false)
        val chatPresenceConfig = chatRepository.getChatPresenceConfig()
        return if (chatPresenceConfig != null && !chatPresenceConfig.isPending) {
            if (needSignalPresence) chatRepository.signalPresenceActivity()
            true
        } else false
    }
}
