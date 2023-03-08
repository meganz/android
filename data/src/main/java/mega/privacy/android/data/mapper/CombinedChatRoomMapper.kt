package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom

/**
 * Combined chat room mapper
 */
internal fun interface CombinedChatRoomMapper {

    operator fun invoke(
        megaChatRoom: MegaChatRoom,
        megaChatListItem: MegaChatListItem,
    ): CombinedChatRoom
}
