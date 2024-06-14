package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.chat.message.CreatePendingAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.ResendMessageUseCase
import javax.inject.Inject

/**
 * Use case to retry the upload of a chat message.
 */
class RetryChatUploadUseCase @Inject constructor(
    private val getPendingMessageUseCase: GetPendingMessageUseCase,
    private val createPendingAttachmentMessageUseCase: CreatePendingAttachmentMessageUseCase,
    private val resendMessageUseCase: ResendMessageUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(chatUploadAppData: List<TransferAppData.ChatUpload>) {
        chatUploadAppData.forEach { appData ->
            getPendingMessageUseCase(appData.pendingMessageId)?.let {
                resendMessageUseCase(createPendingAttachmentMessageUseCase(it))
            }
        }
    }
}