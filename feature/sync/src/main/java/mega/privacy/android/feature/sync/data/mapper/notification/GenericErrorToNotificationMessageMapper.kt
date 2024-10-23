package mega.privacy.android.feature.sync.data.mapper.notification

import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import javax.inject.Inject

internal class GenericErrorToNotificationMessageMapper @Inject constructor() {

    operator fun invoke(
        syncNotificationType: SyncNotificationType,
        issuePath: String = "",
        errorCode: Int = 0
    ): SyncNotificationMessage = SyncNotificationMessage(
        // This will be replaced with transifex strings when UI layer is implemented
        title = "Your syncs have stopped",
        text = "Go to Help Centre to understand why",
        syncNotificationType = syncNotificationType,
        notificationDetails = NotificationDetails(
            path = issuePath,
            errorCode = errorCode
        )
    )
}