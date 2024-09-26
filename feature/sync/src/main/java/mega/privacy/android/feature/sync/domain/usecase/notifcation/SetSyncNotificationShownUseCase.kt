package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import javax.inject.Inject

/**
 * Use case to set the sync notification shown so that the same notification will not be shown again
 */
class SetSyncNotificationShownUseCase @Inject constructor() {

    suspend operator fun invoke(syncNotificationMessage: SyncNotificationMessage) {
        // The logic is not implemented yet
    }
}