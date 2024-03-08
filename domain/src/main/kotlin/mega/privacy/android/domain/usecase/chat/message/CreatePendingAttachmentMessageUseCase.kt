package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import java.io.File
import javax.inject.Inject


/**
 * Creates a chat typed message from a [PendingMessage]]
 */
class CreatePendingAttachmentMessageUseCase @Inject constructor(
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(pendingMessage: PendingMessage) = with(pendingMessage) {
        val file = File(filePath)
        if (isVoiceClip) {
            PendingVoiceClipMessage(
                chatId = chatId,
                msgId = id,
                time = uploadTimestamp,
                isDeletable = true,
                isEditable = false,
                userHandle = getMyUserHandleUseCase(),
                shouldShowAvatar = false,
                reactions = emptyList(),
                status = this.getChatMessageStatus(),
                content = null,
                fileType = fileSystemRepository.getFileTypeInfo(file),
            )
        } else {
            PendingFileAttachmentMessage(
                chatId = chatId,
                msgId = id,
                time = uploadTimestamp,
                isDeletable = true,
                isEditable = false,
                userHandle = getMyUserHandleUseCase(),
                shouldShowAvatar = false,
                reactions = emptyList(),
                status = this.getChatMessageStatus(),
                content = null,
                filePath = filePath,
                fileType = fileSystemRepository.getFileTypeInfo(file),
            )
        }
    }

    private fun PendingMessage.getState() =
        PendingMessageState.entries.firstOrNull { it.value == this.state }


    private fun PendingMessage.getChatMessageStatus() =
        when (getState()) {
            PendingMessageState.ATTACHING -> ChatMessageStatus.SENDING
            PendingMessageState.ERROR_ATTACHING -> ChatMessageStatus.SERVER_REJECTED
            PendingMessageState.SENT -> ChatMessageStatus.DELIVERED
            PendingMessageState.ERROR_UPLOADING -> ChatMessageStatus.SENDING_MANUAL
            else -> ChatMessageStatus.UNKNOWN
        }
}