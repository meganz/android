package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import javax.inject.Inject

/**
 * Use case to get the sync notification message based on occurred stalled issues / errors.
 *
 */
class GetSyncNotificationUseCase @Inject constructor() {

    suspend operator fun invoke(
        stalledIssues: List<StalledIssue>,
        syncs: List<FolderPair>,
    ): SyncNotificationMessage? {
        // The logic is not implemented yet
        return SyncNotificationMessage(
            title = "Sync issues detected",
            text = "View and resolve issues",
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            path = "path",
            errorCode = 0,
        )
    }
}