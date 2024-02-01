package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.domain.entity.chat.messages.meta.ChatRichPreviewInfo
import javax.inject.Inject

/**
 * Rich preview entity mapper
 */
class RichPreviewEntityMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param messageId
     * @param info
     */
    operator fun invoke(messageId: Long, info: ChatRichPreviewInfo) =
        RichPreviewEntity(
            messageId = messageId,
            title = info.title,
            description = info.description,
            image = info.image,
            imageFormat = info.imageFormat,
            icon = info.icon,
            iconFormat = info.iconFormat,
            url = info.url,
            domainName = info.domainName
        )
}