package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.exception.chat.MessageNonDeletableException
import javax.inject.Inject

/**
 * Use case to delete a node attachment message by chatId and msgId.
 */
class DeleteNodeAttachmentMessageByIdsUseCase @Inject constructor(
    private val isMessageDeletableUseCase: IsMessageDeletableUseCase,
    private val revokeAttachmentMessageUseCase: RevokeAttachmentMessageUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long) {
        if (isMessageDeletableUseCase(chatId, msgId)) {
            revokeAttachmentMessageUseCase(chatId, msgId)
        } else {
            throw MessageNonDeletableException()
        }
    }
}