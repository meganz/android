package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessage.Companion.UNKNOWN_TRANSFER_ID
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.file.FileResult
import mega.privacy.android.domain.usecase.file.GetFileSizeFromUriPathUseCase
import javax.inject.Inject


/**
 * Creates a chat typed message from a [PendingMessage]]
 */
class CreatePendingAttachmentMessageUseCase @Inject constructor(
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val getFileSizeFromUriPathUseCase: GetFileSizeFromUriPathUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(pendingMessage: PendingMessage) = with(pendingMessage) {
        val fileName = when {
            uriPath.isPath() -> fileSystemRepository.getFileByPath(uriPath.value)?.name
            else -> fileSystemRepository.getFileNameFromUri(uriPath.value)
        } ?: ""
        val fileTypeInfo = fileSystemRepository.getFileTypeInfo(uriPath, fileName)
        if (isVoiceClip) {
            PendingVoiceClipMessage(
                chatId = chatId,
                msgId = id,
                time = uploadTimestamp,
                isDeletable = true,
                isEditable = false,
                userHandle = getMyUserHandleUseCase(),
                reactions = emptyList(),
                status = this.getChatMessageStatus(),
                content = null,
                uriPath = uriPath,
                fileType = fileTypeInfo,
                transferUniqueId = transferUniqueId.takeIf { it != UNKNOWN_TRANSFER_ID },
                state = PendingMessageState.entries.firstOrNull { it.value == state }
                    ?: PendingMessageState.PREPARING,
                nodeId = nodeHandle.takeIf { it != -1L }?.let { NodeId(it) },
                fileName = name ?: fileName,
            )
        } else {
            val size = (getFileSizeFromUriPathUseCase(uriPath) as? FileResult)?.sizeInBytes ?: 0L
            PendingFileAttachmentMessage(
                chatId = chatId,
                msgId = id,
                time = uploadTimestamp,
                isDeletable = true,
                isEditable = false,
                userHandle = getMyUserHandleUseCase(),
                reactions = emptyList(),
                status = this.getChatMessageStatus(),
                content = null,
                uriPath = uriPath,
                fileType = fileTypeInfo,
                transferUniqueId = transferUniqueId.takeIf { it != UNKNOWN_TRANSFER_ID },
                state = PendingMessageState.entries.firstOrNull { it.value == state }
                    ?: PendingMessageState.PREPARING,
                nodeId = nodeHandle.takeIf { it != -1L }?.let { NodeId(it) },
                fileSize = size,
                fileName = name ?: fileName,
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