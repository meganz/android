package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Archive/Unarchive chat use case
 */
class ArchiveChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Archive/Unarchive chat
     *
     * @param chatId    Chat Id
     * @param archive   Flag to archive/unarchive chat
     */
    suspend operator fun invoke(chatId: Long, archive: Boolean) =
        chatRepository.archiveChat(chatId, archive)
}
