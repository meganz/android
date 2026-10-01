package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to delete a voice clip message.
 */
class DeleteVoiceClipMessageUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val revokeAttachmentMessageUseCase: RevokeAttachmentMessageUseCase,
) : DeleteMessageUseCase() {

    override suspend fun deleteMessage(message: TypedMessage) {
        (message as VoiceClipMessage).let {
            fileSystemRepository.deleteVoiceClip(it.name)
            revokeAttachmentMessageUseCase(it.chatId, it.msgId)
        }
    }

    override suspend fun canDelete(message: TypedMessage) =
        message.isDeletable && message is VoiceClipMessage
}