package mega.privacy.android.feature.sync.domain.repository

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType

/**
 * Repository that stores information about sync notifications
 *
 */
interface SyncNotificationRepository {

    /**
     * Get the displayed notifications by [SyncNotificationType]
     * @return all displayed notifications of a specific type
     */
    suspend fun getDisplayedNotificationsByType(type: SyncNotificationType): List<SyncNotificationMessage>

    /**
     * Set the displayed notification [SyncNotificationMessage]
     */
    suspend fun setDisplayedNotification(notification: SyncNotificationMessage)

    /**
     * Delete the displayed notification by [SyncNotificationType]
     */
    suspend fun deleteDisplayedNotificationByType(type: SyncNotificationType)

    /**
     * Get the battery low notification message
     * @return the battery low notification message
     */
    suspend fun getBatteryLowNotification(): SyncNotificationMessage

    /**
     * Get the user not on wifi notification message
     * @return the user not on wifi notification message
     */
    suspend fun getUserNotOnWifiNotification(): SyncNotificationMessage


    /**
     * Get the sync errors notification message
     * @return the sync errors notification message
     */
    suspend fun getSyncErrorsNotification(syncsWithErrors: List<FolderPair>): SyncNotificationMessage

    /**
     * Get the sync stalled issues notification message
     * @return the sync stalled issues notification message
     */
    suspend fun getSyncStalledIssuesNotification(syncsWithStalledIssues: List<StalledIssue>): SyncNotificationMessage
}