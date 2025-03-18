package mega.privacy.android.feature.sync.data.mapper.notification

import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.shared.resources.R as sharedResR
import javax.inject.Inject

internal class GenericErrorToNotificationMessageMapper @Inject constructor() {

    operator fun invoke(
        syncNotificationType: SyncNotificationType,
        issuePath: String = "",
        errorCode: Int = 0
    ): SyncNotificationMessage = SyncNotificationMessage(
        title = when (syncNotificationType) {
            SyncNotificationType.BATTERY_LOW -> sharedResR.string.general_sync_notification_low_battery_title
            SyncNotificationType.NOT_CONNECTED_TO_WIFI -> sharedResR.string.general_sync_notification_lost_wifi_title
            else -> sharedResR.string.general_sync_notification_generic_error_title
        },
        text = when (syncNotificationType) {
            SyncNotificationType.BATTERY_LOW -> sharedResR.string.general_sync_notification_low_battery_text
            SyncNotificationType.NOT_CONNECTED_TO_WIFI -> sharedResR.string.general_sync_notification_lost_wifi_text
            else -> sharedResR.string.general_sync_notification_generic_error_text
        },
        syncNotificationType = syncNotificationType,
        notificationDetails = NotificationDetails(
            path = issuePath,
            errorCode = errorCode
        )
    )
}