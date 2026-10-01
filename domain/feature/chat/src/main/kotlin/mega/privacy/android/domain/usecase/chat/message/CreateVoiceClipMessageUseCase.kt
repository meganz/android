package mega.privacy.android.domain.usecase.chat.message


import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.chat.AddChatFileTypeUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class CreateVoiceClipMessageUseCase @Inject constructor(
    private val createInvalidMessageUseCase: CreateInvalidMessageUseCase,
    private val addChatFileTypeUseCase: AddChatFileTypeUseCase,
) : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo): TypedMessage = with(request) {
        val fileNode = nodeList.firstOrNull() as? FileNode
            ?: return@with createInvalidMessageUseCase(request)

        val typedNode = addChatFileTypeUseCase(fileNode, chatId, messageId)

        return VoiceClipMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isDeletable = isDeletable,
            isEditable = isEditable,
            isMine = isMine,
            userHandle = userHandle,
            status = status,
            fileNode = typedNode,
            name = typedNode.name,
            size = typedNode.size,
            duration = (typedNode.type as? AudioFileTypeInfo)?.duration ?: 0.seconds,

            reactions = reactions,
            content = content,
            exists = exists,
            rowId = rowId,
        )
    }
}
