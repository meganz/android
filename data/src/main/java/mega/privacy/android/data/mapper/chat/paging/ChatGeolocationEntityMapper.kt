package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.domain.entity.chat.messages.ChatGeolocationInfo
import javax.inject.Inject

/**
 * Chat geolocation entity mapper
 *
 * @constructor Create empty Chat geolocation entity mapper
 */
class ChatGeolocationEntityMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param messageId
     * @param info
     */
    operator fun invoke(
        messageId: Long,
        info: ChatGeolocationInfo,
    ) = ChatGeolocationEntity(
        messageId = messageId,
        latitude = info.latitude,
        longitude = info.longitude,
        image = info.image,
    )
}