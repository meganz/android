package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import javax.inject.Inject

/**
 * Are all contact participants in chat use case
 *
 */
class AreAllContactParticipantsInChatUseCase @Inject constructor(
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
) {
    /**
     * Invoke
     * @return true if all contact participants are in chat, false otherwise
     */
    suspend operator fun invoke(peerHandles: List<Long>): Boolean {
        val participants = peerHandles.toSet()
        return getVisibleContactsUseCase().all { participants.contains(it.handle) }
    }
}