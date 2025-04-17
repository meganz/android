package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.BATTERY_LOW
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.ERROR
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.NOT_CONNECTED_TO_WIFI
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType.STALLED_ISSUE
import javax.inject.Inject

/**
 * Use case to get the sync notification type based on occurred stalled issues / errors.
 */
class GetSyncNotificationTypeUseCase @Inject constructor() {

    /**
     * get the sync notification type based on occurred stalled issues / errors.
     */
    operator fun invoke(
        isBatteryLow: Boolean,
        isUserOnWifi: Boolean,
        isSyncOnlyByWifi: Boolean,
        syncs: List<FolderPair>,
        stalledIssues: List<StalledIssue>,
    ): SyncNotificationType? {
        val isNetworkConstraintRespected = (isSyncOnlyByWifi && isUserOnWifi) || !isSyncOnlyByWifi

        return when {
            syncs.isEmpty() -> {
                null
            }

            isBatteryLow -> {
                BATTERY_LOW
            }

            !isNetworkConstraintRespected -> {
                NOT_CONNECTED_TO_WIFI
            }

            syncs.any { it.syncError != SyncError.NO_SYNC_ERROR } -> {
                ERROR
            }

            stalledIssues.isNotEmpty() -> {
                STALLED_ISSUE
            }

            else -> {
                null
            }
        }
    }
}
