package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.TypedMessageEntity
import mega.privacy.android.domain.entity.chat.messages.paging.MessagePagingInfo
import javax.inject.Inject

/**
 * Message paging info mapper
 *
 * @constructor Create empty Message paging info mapper
 */
class MessagePagingInfoMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param entity
     * @return MessagePagingInfo
     */
    operator fun invoke(entity: TypedMessageEntity): MessagePagingInfo? {
        return MessagePagingInfo(
            userHandle = entity.userHandle,
            isMine = entity.isMine,
            timestamp = entity.timestamp

        )
    }
}