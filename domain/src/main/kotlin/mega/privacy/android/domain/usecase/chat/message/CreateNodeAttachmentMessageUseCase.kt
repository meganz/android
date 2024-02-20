package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.chat.AddChatFileTypeUseCase
import javax.inject.Inject


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
            isMine = isMine,
            userHandle = userHandle,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            fileNode = typedNode,
            reactions = reactions,
        )
    }
}