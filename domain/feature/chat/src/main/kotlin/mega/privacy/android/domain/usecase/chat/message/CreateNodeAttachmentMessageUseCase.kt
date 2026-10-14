package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.chat.AddChatFileTypeUseCase
import javax.inject.Inject

/**
 * Creates a [NodeAttachmentMessage] from the first node of an attachment message.
 *
 * Only the first node is used, because an attachment message is expected to carry one node.
 * `MegaChatApi.attachNodes` is the only SDK call that puts several nodes in one message. It has
 * been deprecated for a long time, and clients send one message for each node. A message with
 * several nodes can only come from an old client, and the nodes after the first one are not shown.
 */
internal class CreateNodeAttachmentMessageUseCase @Inject constructor(
    private val createInvalidMessageUseCase: CreateInvalidMessageUseCase,
    private val addChatFileTypeUseCase: AddChatFileTypeUseCase,
) : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        val fileNode = nodeList.firstOrNull() as? FileNode
            ?: return@with createInvalidMessageUseCase(request)

        val typedNode = addChatFileTypeUseCase(fileNode, chatId, messageId)

        NodeAttachmentMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isDeletable = isDeletable,
            isEditable = isEditable,
            isMine = isMine,
            userHandle = userHandle,
            fileNode = typedNode,
            reactions = reactions,
            status = status,
            content = content,
            exists = exists,
            rowId = rowId,
        )
    }
}