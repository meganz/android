package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.UserChatStatus
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * Mapper to convert data into [UserChatStatus].
 */
internal class UserChatStatusMapper @Inject constructor() {

    private val userChatStatus = mapOf(
        MegaChatApi.STATUS_OFFLINE to UserChatStatus.Offline,
        MegaChatApi.STATUS_AWAY to UserChatStatus.Away,
        MegaChatApi.STATUS_ONLINE to UserChatStatus.Online,
        MegaChatApi.STATUS_BUSY to UserChatStatus.Busy,
        MegaChatApi.STATUS_INVALID to UserChatStatus.Invalid,
    )

    /**
     * Invoke.
     *
     * @param status User chat status.
     * @return [UserChatStatus]
     */
    operator fun invoke(status: Int) = userChatStatus[status] ?: UserChatStatus.Invalid
}