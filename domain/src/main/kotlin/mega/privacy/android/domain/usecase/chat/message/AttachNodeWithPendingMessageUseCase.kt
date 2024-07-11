package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndNodeHandleRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.GetPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import java.io.File
import javax.inject.Inject

/**
 * Attach a Node with the information contained in a PendingMessage once the file is uploaded
 * It will update the status of the pending message and delete it if everything goes right
 */
class AttachNodeWithPendingMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatRepository: ChatRepository,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
    private val setNodeAttributesAfterUploadUseCase: SetNodeAttributesAfterUploadUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
    private val getPendingMessageUseCase: GetPendingMessageUseCase,
) {
    /**
     * Invoke
     *
     * @param pendingMessageId
     * @param nodeId of the already uploaded file that will be attached to the chat
     */
    suspend operator fun invoke(pendingMessageId: Long, nodeId: NodeId) {

        getPendingMessageUseCase(pendingMessageId)
            ?.let { pendingMessage ->
                val originalPath =
                    chatMessageRepository.getCachedOriginalPathForPendingMessage(pendingMessage.id)
                chatMessageRepository.cacheOriginalPathForNode(
                    nodeId,
                    originalPath ?: pendingMessage.filePath
                )
                updatePendingMessageUseCase(
                    UpdatePendingMessageStateAndNodeHandleRequest(
                        pendingMessageId = pendingMessage.id,
                        state = PendingMessageState.ATTACHING,
                        nodeHandle = nodeId.longValue,
                    )
                )
                runCatching {
                    setNodeAttributesAfterUploadUseCase(
                        nodeId.longValue,
                        File(pendingMessage.filePath)
                    )
                }
                val chatId = pendingMessage.chatId
                val attachedNode = if (pendingMessage.isVoiceClip) {
                    chatMessageRepository.attachVoiceMessage(chatId, nodeId.longValue)
                } else {
                    chatMessageRepository.attachNode(chatId, nodeId)
                }
                if (attachedNode != null) {
                    getChatMessageUseCase(chatId, attachedNode)?.let { message ->
                        val request = createSaveSentMessageRequestUseCase(message, chatId)
                        chatRepository.storeMessages(listOf(request))
                    }
                    chatMessageRepository.deletePendingMessage(pendingMessage)
                } else {
                    updatePendingMessageUseCase(
                        UpdatePendingMessageStateRequest(
                            pendingMessageId = pendingMessage.id,
                            state = PendingMessageState.ERROR_ATTACHING,
                        )
                    )
                }
            }
    }
}