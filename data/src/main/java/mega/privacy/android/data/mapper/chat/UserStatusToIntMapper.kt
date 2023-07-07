package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.contacts.UserStatus
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
    operator fun invoke(status: UserStatus) = when (status) {
        UserStatus.Offline -> MegaChatApi.STATUS_OFFLINE
        UserStatus.Away -> MegaChatApi.STATUS_AWAY
        UserStatus.Online -> MegaChatApi.STATUS_ONLINE
        UserStatus.Busy -> MegaChatApi.STATUS_BUSY
        UserStatus.Invalid -> MegaChatApi.STATUS_INVALID
    }
}