package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get the list of active chat items
 *
 * @property chatRepository
 */
class GetActiveChatListItemsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @return List of active chat items.
     */
    suspend operator fun invoke() = chatRepository.getActiveChatListItems()
}
