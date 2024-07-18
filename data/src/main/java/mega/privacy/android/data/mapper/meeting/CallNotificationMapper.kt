package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.call.CallNotificationType
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert call notification to [CallNotificationType]
 */
internal class CallNotificationMapper @Inject constructor() {
    operator fun invoke(notification: Int): CallNotificationType = when (notification) {
        MegaChatCall.NOTIFICATION_TYPE_SFU_ERROR -> CallNotificationType.SFUError
        MegaChatCall.NOTIFICATION_TYPE_SFU_DENY -> CallNotificationType.SFUDeny
        else -> CallNotificationType.Invalid
    }
}