package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.ChatRoomPermission
import nz.mega.sdk.MegaChatRoom

/**
 * Mapper to convert permission data to [ChatRoomPermission]
 */
typealias ChatPermissionsMapper = (
    @JvmSuppressWildcards Int,
) -> ChatRoomPermission

internal fun toChatRoomPermission(privilege: Int) =
    userPermission[privilege] ?: ChatRoomPermission.Unknown

/**
 * User permissions
 */
val userPermission = mapOf(
    MegaChatRoom.PRIV_RM to ChatRoomPermission.Removed,
    MegaChatRoom.PRIV_RO to ChatRoomPermission.ReadOnly,
    MegaChatRoom.PRIV_STANDARD to ChatRoomPermission.Standard,
    MegaChatRoom.PRIV_MODERATOR to ChatRoomPermission.Moderator,
    MegaChatRoom.PRIV_UNKNOWN to ChatRoomPermission.Unknown
)