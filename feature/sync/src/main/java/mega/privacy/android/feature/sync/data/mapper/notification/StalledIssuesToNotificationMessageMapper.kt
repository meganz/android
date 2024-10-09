package mega.privacy.android.feature.sync.data.mapper.notification

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import javax.inject.Inject

internal class StalledIssuesToNotificationMessageMapper @Inject constructor() {

    operator fun invoke(): SyncNotificationMessage = SyncNotificationMessage(
        // This will be replaced with transifex strings when UI layer is implemented
        title = "Sync error",
        text = "View and resolve issues",
        syncNotificationType = SyncNotificationType.ERROR,
        path = "path",
        errorCode = 0,
    )
}