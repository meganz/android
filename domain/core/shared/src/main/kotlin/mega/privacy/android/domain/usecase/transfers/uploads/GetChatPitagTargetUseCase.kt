package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get chat [PitagTarget] given a list of pending messages.
 */
class GetChatPitagTargetUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     */
    suspend operator fun invoke(pendingMessages: List<PendingMessage>): PitagTarget =
        when {
            pendingMessages.isEmpty() -> PitagTarget.NotApplicable
            pendingMessages.size > 1 -> PitagTarget.MultipleChats
            chatRepository.isNoteToSelfChat(pendingMessages.first().chatId) == true -> PitagTarget.NoteToSelf
            else -> chatRepository.isGroupChat(pendingMessages.first().chatId)?.let { isGroup ->
                if (isGroup) PitagTarget.ChatGroup else PitagTarget.Chat1To1
            } ?: PitagTarget.NotApplicable
        }
}