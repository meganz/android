package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get the list of archived chat items
 *
 * @property chatRepository
 */
class GetArchivedChatListItemsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @return List of archived chat items
     */
    suspend operator fun invoke() = chatRepository.getArchivedChatListItems()
}
