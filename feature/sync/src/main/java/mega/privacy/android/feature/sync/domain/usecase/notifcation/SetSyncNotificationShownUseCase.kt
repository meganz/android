package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import javax.inject.Inject

/**
 * Use case to set the sync notification shown so that the same notification will not be shown again
 */
class SetSyncNotificationShownUseCase @Inject constructor(
    private val syncNotificationRepository: SyncNotificationRepository,
) {

    suspend operator fun invoke(
        syncNotificationMessage: SyncNotificationMessage,
        notificationId: Int?
    ) {
        syncNotificationRepository.setDisplayedNotification(
            notification = syncNotificationMessage,
            notificationId = notificationId,
        )
    }
}