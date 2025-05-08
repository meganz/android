package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Permission repository
 *
 */
interface PermissionRepository {
    /**
     * Has media permission
     */
    fun hasMediaPermission(): Boolean

    /**
     * Has audio permission
     */
    fun hasAudioPermission(): Boolean

    /**
     * Has manage external storage permission
     */
    fun hasManageExternalStoragePermission(): Boolean

    /**
     * Has location permission
     */
    fun isLocationPermissionGranted(): Boolean

    /**
     * Set notification permission shown and save its timestamp
     *
     * @param timestamp The timestamp when the notification permission was shown
     */
    suspend fun setNotificationPermissionShownTimestamp(timestamp: Long)

    /**
     * Monitor the last timestamp when the notification permission was shown
     */
    suspend fun monitorNotificationPermissionShownTimestamp(): Flow<Long?>
}