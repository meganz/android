package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatParticipantsRepository
import javax.inject.Inject

/**
 * Load user attributes use case
 *
 * @property chatParticipantsRepository [ChatParticipantsRepository]
 */
class LoadUserAttributesUseCase @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @param usersHandles List of user handles.
     */
    suspend operator fun invoke(chatId: Long, usersHandles: List<Long>) =
        chatParticipantsRepository.loadUserAttributes(chatId, usersHandles)
}