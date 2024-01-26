package mega.privacy.android.domain.usecase.chat.message


import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.FileNode
import javax.inject.Inject

internal class CreateVoiceClipMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest): TypedMessage = with(request) {

        val status: ChatMessageStatus = message.status

        var name = ""
        var size = 0L
        var duration = 0
        message.nodeList.firstOrNull()?.let {
            (it as? FileNode)?.let { fileNode ->
                name = fileNode.name
                size = fileNode.size
                duration = (fileNode.type as? AudioFileTypeInfo)?.duration ?: 0
            }
        }

        return VoiceClipMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle,
            status = status,
            duration = duration,
            name = name,
            size = size,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}
