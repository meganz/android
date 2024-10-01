package mega.privacy.android.feature.sync.domain.usecase.notifcation

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import javax.inject.Inject

/**
 * Use case to get the sync notification message based on occurred stalled issues / errors.
 *
 */
class GetSyncNotificationUseCase @Inject constructor(
    private val syncNotificationRepository: SyncNotificationRepository,
) {

    /**
     * Follow the business flow to decide if a sync notification should be displayed.
     * If the sync notification is not required, null is returned
     */
    suspend operator fun invoke(
        isSyncNotificationDisplayed: Boolean,
        isBatteryLow: Boolean,
        isUserOnWifi: Boolean,
        isSyncOnlyByWifi: Boolean,
        syncs: List<FolderPair>,
        stalledIssues: List<StalledIssue>,
    ): SyncNotificationMessage? {
        val isNetworkConstraintRespected = isSyncOnlyByWifi && isUserOnWifi || !isSyncOnlyByWifi

        return when {
            isSyncNotificationDisplayed -> null
            isBatteryLow -> getBatteryLowNotification()
            !isNetworkConstraintRespected -> getNetworkConstraintNotification()
            syncs.any { it.syncError != null } -> getSyncErrorsNotification(syncs)
            stalledIssues.isNotEmpty() -> getStalledIssuesNotification(stalledIssues)
            else -> null
        }
    }

    private suspend fun getBatteryLowNotification(): SyncNotificationMessage? {
        return if (!syncNotificationRepository.isBatteryLowNotificationShown()) {
            syncNotificationRepository.setBatteryLowNotificationShown(true)
            syncNotificationRepository.getBatteryLowNotification()
        } else {
            null
        }
    }

    private suspend fun getNetworkConstraintNotification(): SyncNotificationMessage? {
        return if (!syncNotificationRepository.isUserNotOnWifiNotificationShown()) {
            syncNotificationRepository.setUserNotOnWifiNotificationShown(true)
            syncNotificationRepository.getUserNotOnWifiNotification()
        } else {
            null
        }
    }

    private suspend fun getSyncErrorsNotification(syncs: List<FolderPair>): SyncNotificationMessage? {
        return if (!syncNotificationRepository.isSyncErrorsNotificationShown(syncs)) {
            syncNotificationRepository.setSyncErrorsNotificationShown(syncs)
            syncNotificationRepository.getSyncErrorsNotification()
        } else {
            null
        }
    }

    private suspend fun getStalledIssuesNotification(stalledIssues: List<StalledIssue>): SyncNotificationMessage? {
        return if (!syncNotificationRepository.isSyncStalledIssuesNotificationShown(stalledIssues)) {
            syncNotificationRepository.setSyncStalledIssuesNotificationShown(stalledIssues)
            syncNotificationRepository.getSyncStalledIssuesNotification()
        } else {
            null
        }
    }
}