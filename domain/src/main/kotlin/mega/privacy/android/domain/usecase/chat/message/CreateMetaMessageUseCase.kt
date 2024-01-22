package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

/**
 * Create meta message use case.
 */
class CreateMetaMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) = with(request) {
        when (metaType) {
            ContainsMetaType.RICH_PREVIEW -> RichPreviewMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                chatRichPreviewInfo = chatRichPreviewInfo,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            ContainsMetaType.GEOLOCATION -> LocationMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                chatGeolocationInfo = chatGeolocationInfo,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )

            ContainsMetaType.GIPHY -> GiphyMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                chatGifInfo = chatGifInfo,
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