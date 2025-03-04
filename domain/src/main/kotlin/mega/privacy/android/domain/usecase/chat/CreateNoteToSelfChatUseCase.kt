package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject


/**
 * Use case to create note to self chat
 */
class CreateNoteToSelfChatUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    /**
     * Invoke.
     *
     * @return [ChatRoom]   containing the updated data.
     */
    suspend operator fun invoke() = chatRepository.createChat(isGroup = false, userHandles = null)
}