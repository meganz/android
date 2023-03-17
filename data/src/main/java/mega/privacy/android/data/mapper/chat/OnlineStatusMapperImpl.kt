package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserStatus
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * Implementation of [OnlineStatusMapper].
 */
internal class OnlineStatusMapperImpl @Inject constructor() : OnlineStatusMapper {

    override fun invoke(userHandle: Long, status: Int, inProgress: Boolean) =
        OnlineStatus(userHandle, userStatus[status] ?: UserStatus.Invalid, inProgress)

    companion object {
        val userStatus = mapOf(
            MegaChatApi.STATUS_OFFLINE to UserStatus.Offline,
            MegaChatApi.STATUS_AWAY to UserStatus.Away,
            MegaChatApi.STATUS_ONLINE to UserStatus.Online,
            MegaChatApi.STATUS_BUSY to UserStatus.Busy,
            MegaChatApi.STATUS_INVALID to UserStatus.Invalid,
        )
    }
}