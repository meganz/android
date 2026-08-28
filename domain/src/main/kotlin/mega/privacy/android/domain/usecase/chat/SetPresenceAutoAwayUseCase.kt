package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set presence auto-away use case
 *
 * @property chatRepository
 */
class SetPresenceAutoAwayUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param enabled           True to enable the auto-away feature
     * @param timeoutSeconds    Seconds to wait before turning away
     */
    suspend operator fun invoke(enabled: Boolean, timeoutSeconds: Int) =
        chatRepository.setPresenceAutoAway(enabled, timeoutSeconds)
}
