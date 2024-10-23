package mega.privacy.android.feature.sync.data.mapper.notification

import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import javax.inject.Inject

internal class StalledIssuesToNotificationMessageMapper @Inject constructor() {

    operator fun invoke(
        issuePath: String,
    ): SyncNotificationMessage = SyncNotificationMessage(
        // This will be replaced with transifex strings when UI layer is implemented
        title = "Sync issues detected",
        text = "View and resolve issues",
        syncNotificationType = SyncNotificationType.STALLED_ISSUE,
        notificationDetails = NotificationDetails(
            path = issuePath,
            errorCode = 0
        )
    )
}