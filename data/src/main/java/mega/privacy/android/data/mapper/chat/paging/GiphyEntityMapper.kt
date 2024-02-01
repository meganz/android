package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import javax.inject.Inject

/**
 * Giphy entity mapper
 *
 * @constructor Create empty Giphy entity mapper
 */
class GiphyEntityMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param messageId
     * @param info
     */
    operator fun invoke(
        messageId: Long,
        info: ChatGifInfo,
    ) = GiphyEntity(
        messageId = messageId,
        mp4Src = info.mp4Src,
        webpSrc = info.webpSrc,
        title = info.title,
        mp4Size = info.mp4Size,
        webpSize = info.webpSize,
        width = info.width,
        height = info.height
    )
}