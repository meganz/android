package mega.privacy.android.domain.usecase.chat.message


import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.FileNode
import javax.inject.Inject

internal class CreateVoiceClipMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest): TypedMessage = with(request) {
        val fileNode = message.nodeList.firstOrNull() as? FileNode
        return VoiceClipMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle,
            status = message.status,
            name = fileNode?.name.orEmpty(),
            size = fileNode?.size ?: 0L,
            duration = (fileNode?.type as? AudioFileTypeInfo)?.duration ?: 0,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}
