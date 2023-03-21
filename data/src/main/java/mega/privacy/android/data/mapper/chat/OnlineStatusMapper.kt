package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserStatus
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * Mapper to convert data into [OnlineStatus].
 */
internal class OnlineStatusMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     * @param status User chat status.
     * @param inProgress Whether the reported status is being set or it is definitive (only for your own changes).
     * @return [OnlineStatus]
     */
    operator fun invoke(userHandle: Long, status: Int, inProgress: Boolean) =
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