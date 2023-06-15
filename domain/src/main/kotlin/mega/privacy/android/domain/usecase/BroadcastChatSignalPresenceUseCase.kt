package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use Case to Broadcast if chat is in presence of a Network Signal
 */
class BroadcastChatSignalPresenceUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {
    /**
     * Invoke to broadcast and notify app event of chat network signal
     */
    suspend operator fun invoke() = networkRepository.broadcastChatSignalPresence()
}