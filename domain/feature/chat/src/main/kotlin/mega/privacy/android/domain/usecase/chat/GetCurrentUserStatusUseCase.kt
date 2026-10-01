package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatParticipantsRepository
import javax.inject.Inject

/**
 * Get user status use case
 *
 * @property chatParticipantsRepository
 */
class GetCurrentUserStatusUseCase @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
) {

    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = chatParticipantsRepository.getCurrentStatus()
}
