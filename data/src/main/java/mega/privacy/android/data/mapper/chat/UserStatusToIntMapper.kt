package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.contacts.UserChatStatus
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * User status to int mapper
 *
 */
class UserStatusToIntMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param status
     */
    operator fun invoke(status: UserChatStatus) = when (status) {
        UserChatStatus.Offline -> MegaChatApi.STATUS_OFFLINE
        UserChatStatus.Away -> MegaChatApi.STATUS_AWAY
        UserChatStatus.Online -> MegaChatApi.STATUS_ONLINE
        UserChatStatus.Busy -> MegaChatApi.STATUS_BUSY
        UserChatStatus.Invalid -> MegaChatApi.STATUS_INVALID
    }
}