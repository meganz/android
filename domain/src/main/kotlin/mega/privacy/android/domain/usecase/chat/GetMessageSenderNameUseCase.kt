package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.usecase.account.GetUserAliasUseCase
import javax.inject.Inject

/**
 * Use case for getting the sender name of a message in chat.
 */
class GetMessageSenderNameUseCase @Inject constructor(
    private val getUserAliasUseCase: GetUserAliasUseCase,
    private val participantsRepository: ChatParticipantsRepository,
) {

    /**
     * Invoke.
     *
     * @param userHandle The sender user handle.
     * @param chatId The chat identifier the message belongs to.
     * @return The sender name if any.
     */
    suspend fun invoke(userHandle: Long, chatId: Long) =
        getUserAliasUseCase(userHandle) ?: getSenderName(userHandle, chatId)

    private suspend fun getSenderName(userHandle: Long, chatId: Long): String? {
        participantsRepository.loadUserAttributes(chatId, listOf(userHandle))
        return participantsRepository.getUserFullNameFromCache(userHandle)
            ?: participantsRepository.getUserEmailFromCache(userHandle)
    }
}