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
     * @return true if the notification has been shown, false otherwise
     */
    suspend fun isBatteryLowNotificationShown(): Boolean

    /**
     * Get the battery low notification message
     * @return the battery low notification message
     */
    suspend fun getBatteryLowNotification(): SyncNotificationMessage

    /**
     * Set that the battery low notification has been shown to the user
     * @param shown true if we need to set notification as shown, false if we need to delete it
     */
    suspend fun setBatteryLowNotificationShown(shown: Boolean)

    /**
     * Check if the user not on wifi notification has been already shown to the user
     * @return true if the notification has been shown, false otherwise
     */
    suspend fun isUserNotOnWifiNotificationShown(): Boolean

    /**
     * Set that the user not on wifi notification has been shown to the user
     * @param shown true if we need to set notification as shown, false if we need to delete it
     *
     */
    suspend fun setUserNotOnWifiNotificationShown(shown: Boolean)

    /**
     * Get the user not on wifi notification message
     * @return the user not on wifi notification message
     */
    suspend fun getUserNotOnWifiNotification(): SyncNotificationMessage

    /**
     * Check if the sync errors notification has been already shown to the user
     * @param syncs to check if the notification associated with specified syncs have been shown
     * @return true if the notification has been shown, false otherwise
     */
    suspend fun isSyncErrorsNotificationShown(syncs: List<FolderPair>): Boolean

    /**
     * Set that the sync errors notification has been shown to the user
     * @param syncs list of syncs to set the notification as shown for
     * @param shown set to true to mark specified syncs as shown, false if we need to delete them
     */
    suspend fun setSyncErrorsNotificationShown(syncs: List<FolderPair>, shown: Boolean)

    /**
     * Get the sync errors notification message
     * @return the sync errors notification message
     */
    suspend fun getSyncErrorsNotification(): SyncNotificationMessage

    /**
     * Check if the stalled issues notification associated with specified syncs have been shown
     * @param stalledIssues to check if the notification associated with specified stalled issues has been shown
     */
    suspend fun isSyncStalledIssuesNotificationShown(stalledIssues: List<StalledIssue>): Boolean

    /**
     * Set that the sync stalled issues notification has been shown to the user
     * @param stalledIssues list of stalled issues to set the notification as shown for
     * @param shown set to true to mark specified stalled issues as shown, false if we need to delete them
     */
    suspend fun setSyncStalledIssuesNotificationShown(
        stalledIssues: List<StalledIssue>,
        shown: Boolean,
    )

    /**
     * Get the sync stalled issues notification message
     * @return the sync stalled issues notification message
     */
    suspend fun getSyncStalledIssuesNotification(): SyncNotificationMessage
}