package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import javax.inject.Inject

/**
 * Use case to get the sync issue notification by type
 */
class GetSyncIssueNotificationByTypeUseCase @Inject constructor(
    private val syncNotificationRepository: SyncNotificationRepository,
) {

    operator fun invoke(type: SyncNotificationType) =
        syncNotificationRepository.getSyncIssueNotificationByType(type)
}
