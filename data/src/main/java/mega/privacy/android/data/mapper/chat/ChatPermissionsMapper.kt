package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject

/**
 * Mapper to convert permission data to [ChatRoomPermission]
 */
internal class ChatPermissionsMapper @Inject constructor() {
    operator fun invoke(privilege: Int?): ChatRoomPermission =
        userPermission[privilege] ?: ChatRoomPermission.Unknown

    companion object {
        internal val userPermission = mapOf(
            MegaChatRoom.PRIV_RM to ChatRoomPermission.Removed,
            MegaChatRoom.PRIV_RO to ChatRoomPermission.ReadOnly,
            MegaChatRoom.PRIV_STANDARD to ChatRoomPermission.Standard,
            MegaChatRoom.PRIV_MODERATOR to ChatRoomPermission.Moderator,
            MegaChatRoom.PRIV_UNKNOWN to ChatRoomPermission.Unknown
        )
    }
}