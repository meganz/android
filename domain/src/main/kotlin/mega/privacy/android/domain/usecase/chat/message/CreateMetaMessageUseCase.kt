package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import javax.inject.Inject

internal class CreateMetaMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) = with(request) {
        when (metaType) {
            ContainsMetaType.RICH_PREVIEW -> RichPreviewMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                preview = richPreview,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            ContainsMetaType.GEOLOCATION -> LocationMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                geolocation = geolocation,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            ContainsMetaType.GIPHY -> GiphyMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                giphy = giphy,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            else -> InvalidMetaMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )
        }
    }
}