package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserStatus
import nz.mega.sdk.MegaChatApi

/**
 * Mapper to convert OnChatInitStateUpdate data to [OnlineStatus]
 */
typealias OnlineStatusMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Boolean,
) -> OnlineStatus

internal fun toOnlineStatus(userHandle: Long, status: Int, inProgress: Boolean) =
    OnlineStatus(userHandle, userStatus[status] ?: UserStatus.Invalid, inProgress)

val userStatus = mapOf(
    MegaChatApi.STATUS_OFFLINE to UserStatus.Offline,
    MegaChatApi.STATUS_AWAY to UserStatus.Away,
    MegaChatApi.STATUS_ONLINE to UserStatus.Online,
    MegaChatApi.STATUS_BUSY to UserStatus.Busy,
    MegaChatApi.STATUS_INVALID to UserStatus.Invalid
)