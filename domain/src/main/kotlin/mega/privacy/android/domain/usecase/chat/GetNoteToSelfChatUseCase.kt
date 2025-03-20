package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case get note to self chat
 */
class GetNoteToSelfChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @return [ChatRoom]   containing the updated data.
     */
    suspend operator fun invoke(): ChatRoom? {
        return chatRepository.getNoteToSelfChat().let { chatRoom ->
            chatRoom
                ?: chatRepository.createChat(isGroup = false, userHandles = null).let {
                    chatRepository.getNoteToSelfChat()
                }
        }
    }
}
