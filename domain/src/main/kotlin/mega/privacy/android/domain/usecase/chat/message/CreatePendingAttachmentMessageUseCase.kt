package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import java.io.File
import javax.inject.Inject


/**
 * Creates a chat typed message from a [PendingMessage]]
 */
class CreatePendingAttachmentMessageUseCase @Inject constructor(
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(pendingMessage: PendingMessage) = with(pendingMessage) {
        PendingAttachmentMessage(
            chatId = chatId,
            msgId = id,
            time = uploadTimestamp,
            isDeletable = false,
            isEditable = false,
            userHandle = getMyUserHandleUseCase(),
            shouldShowAvatar = true,
            shouldShowTime = true,
            reactions = emptyList(),
            status = this.getChatMessageStatus(),
            file = File(filePath),
            fileType = UnknownFileTypeInfo("", "")
        )
    }


    private fun PendingMessage.getChatMessageStatus(): ChatMessageStatus {
        val pendingMessageState =
            PendingMessageState.entries.firstOrNull { it.value == this.state }
                ?: return ChatMessageStatus.UNKNOWN
        return when (pendingMessageState) {
            PendingMessageState.PREPARING -> ChatMessageStatus.UNKNOWN
            PendingMessageState.PREPARING_FROM_EXPLORER -> ChatMessageStatus.UNKNOWN
            PendingMessageState.UPLOADING -> ChatMessageStatus.UNKNOWN
            PendingMessageState.ATTACHING -> ChatMessageStatus.SENDING
            PendingMessageState.COMPRESSING -> ChatMessageStatus.UNKNOWN
            PendingMessageState.SENT -> ChatMessageStatus.DELIVERED
            PendingMessageState.ERROR_UPLOADING -> ChatMessageStatus.UNKNOWN
            PendingMessageState.ERROR_ATTACHING -> ChatMessageStatus.SERVER_REJECTED
        }
    }
}