package mega.privacy.android.feature.sync.domain.repository

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage

/**
 * Repository that stores information about sync notifications
 *
 */
interface SyncNotificationRepository {

    /**
     * Check if the battery low notification has been already shown to the user
     */
    suspend fun isBatteryLowNotificationShown(): Boolean

    /**
     * Get the battery low notification message
     */
    suspend fun getBatteryLowNotification(): SyncNotificationMessage

    /**
     * Set that the battery low notification has been shown to the user
     */
    suspend fun setBatteryLowNotificationShown(shown: Boolean)

    /**
     * Check if the user not on wifi notification has been already shown to the user
     */
    suspend fun isUserNotOnWifiNotificationShown(): Boolean

    /**
     * Set that the user not on wifi notification has been shown to the user
     */
    suspend fun setUserNotOnWifiNotificationShown(shown: Boolean)

    /**
     * Get the user not on wifi notification message
     */
    suspend fun getUserNotOnWifiNotification(): SyncNotificationMessage

    /**
     * Check if the sync errors notification has been already shown to the user
     */
    suspend fun isSyncErrorsNotificationShown(syncs: List<FolderPair>): Boolean

    /**
     * Set that the sync errors notification has been shown to the user
     */
    suspend fun setSyncErrorsNotificationShown(syncs: List<FolderPair>)

    /**
     * Get the sync errors notification message
     */
    suspend fun getSyncErrorsNotification(): SyncNotificationMessage

    /**
     * Check if the sync stalled issues notification has been already shown to the user
     */
    suspend fun isSyncStalledIssuesNotificationShown(stalledIssues: List<StalledIssue>): Boolean

    /**
     * Set that the sync stalled issues notification has been shown to the user
     */
    suspend fun setSyncStalledIssuesNotificationShown(stalledIssues: List<StalledIssue>)

    /**
     * Get the sync stalled issues notification message
     */
    suspend fun getSyncStalledIssuesNotification(): SyncNotificationMessage
}