package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import javax.inject.Inject

internal class CreateMetaMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) = with(request) {
        when (message.containsMeta?.type) {
            ContainsMetaType.RICH_PREVIEW -> RichPreviewMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                preview = message.containsMeta.richPreview,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            ContainsMetaType.GEOLOCATION -> LocationMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                geolocation = message.containsMeta.geolocation,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            ContainsMetaType.GIPHY -> GiphyMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                giphy = message.containsMeta.giphy,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            else -> InvalidMetaMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )
        }
    }
}