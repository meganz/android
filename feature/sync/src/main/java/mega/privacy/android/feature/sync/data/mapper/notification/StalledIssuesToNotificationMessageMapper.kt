package mega.privacy.android.feature.sync.data.mapper.notification

import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.shared.resources.R as sharedResR
import javax.inject.Inject

internal class StalledIssuesToNotificationMessageMapper @Inject constructor() {

    operator fun invoke(
        issuePath: String,
    ): SyncNotificationMessage = SyncNotificationMessage(
        title = sharedResR.string.general_sync_notification_stalled_issues_title,
        text = sharedResR.string.general_sync_notification_stalled_issues_text,
        syncNotificationType = SyncNotificationType.STALLED_ISSUE,
        notificationDetails = NotificationDetails(
            path = issuePath,
            errorCode = 0
        )
    )
}