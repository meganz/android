package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import javax.inject.Inject

internal class CreateMetaMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = when (message.containsMeta?.type) {
        ContainsMetaType.RICH_PREVIEW -> RichPreviewMessage(
            message.msgId,
            message.timestamp,
            isMine = isMine
        )

        ContainsMetaType.GEOLOCATION -> RichPreviewMessage(
            message.msgId,
            message.timestamp,
            isMine = isMine
        )

        ContainsMetaType.GIPHY -> GiphyMessage(
            message.msgId,
            message.timestamp,
            isMine = isMine
        )

        else -> InvalidMessage(
            message.msgId,
            message.timestamp,
            isMine = isMine
        )
    }
}