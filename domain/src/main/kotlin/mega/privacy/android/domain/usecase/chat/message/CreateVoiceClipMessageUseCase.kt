package mega.privacy.android.domain.usecase.chat.message


import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.node.FileNode
import javax.inject.Inject

internal class CreateVoiceClipMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo): TypedMessage = with(request) {
        val fileNode = nodeList.firstOrNull() as? FileNode
        return VoiceClipMessage(
            msgId = msgId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            status = status,
            name = fileNode?.name.orEmpty(),
            size = fileNode?.size ?: 0L,
            duration = (fileNode?.type as? AudioFileTypeInfo)?.duration?.let { it * 1000 } ?: 0,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}
