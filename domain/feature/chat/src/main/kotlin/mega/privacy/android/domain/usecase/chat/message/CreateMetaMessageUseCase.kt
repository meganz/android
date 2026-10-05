package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.usecase.chat.GetLinkTypesUseCase
import javax.inject.Inject

/**
 * Create meta message use case.
 */
class CreateMetaMessageUseCase @Inject constructor(
    private val getLinkTypesUseCase: GetLinkTypesUseCase,
) : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo): TypedMessage = with(request) {
        when (metaType) {
            ContainsMetaType.RICH_PREVIEW -> createRichPreviewMessage()

            ContainsMetaType.GEOLOCATION -> LocationMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                chatGeolocationInfo = chatGeolocationInfo,

                reactions = reactions,
                isEdited = isEdited,
                status = status,
                content = content,
                rowId = rowId,
            )

            ContainsMetaType.GIPHY -> GiphyMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                chatGifInfo = chatGifInfo,

                reactions = reactions,
                status = status,
                content = content,
                rowId = rowId,
            )

            else -> InvalidMetaMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,

                reactions = reactions,
                status = status,
                content = content,
                rowId = rowId,
            )
        }
    }

    /**
     * The chat SDK attaches a generic web preview to autoplay links, but the app opens them in
     * the web view, so the preview card would misrepresent them. Show the raw link instead.
     */
    private fun CreateTypedMessageInfo.createRichPreviewMessage(): TypedMessage =
        if (hasAutoplayLink()) {
            TextMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                reactions = reactions,
                status = status,
                content = content.orEmpty(),
                rowId = rowId,
                isEdited = false,
                hasOtherLink = false,
            )
        } else {
            RichPreviewMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,
                chatRichPreviewInfo = chatRichPreviewInfo,

                reactions = reactions,
                content = content.orEmpty(),
                // Rich preview messages are always edited because are sent as normal messages,
                // but then edited after getting the preview info from API.
                // So we cannot manage any real edited status.
                isEdited = false,
                status = status,
                rowId = rowId,
            )
        }

    private fun CreateTypedMessageInfo.hasAutoplayLink() =
        getLinkTypesUseCase(content.orEmpty()).any { it.type == RegexPatternType.AUTOPLAY_LINK }
}