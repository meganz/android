package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for storing user preference regarding Sync
 */
interface SyncPreferencesRepository {

    /**
     * Sets if sync should be done only when connected to WiFi
     */
    suspend fun setSyncByWiFi(checked: Boolean)

    /**
     * Gets if sync should be done only when connected to WiFi
     */
    fun monitorSyncByWiFi(): Flow<Boolean?>

    /**
     * Sets if sync should be done only when charging
     */
    suspend fun setSyncByCharging(checked: Boolean)

    /**
     * Gets if sync should be done only when charging
     */
    fun monitorSyncByCharging(): Flow<Boolean?>

    /**
     * Sets if sync should be paused when the device is in power save mode (battery saver)
     */
    suspend fun setPauseSyncOnBatterySaver(checked: Boolean)

    /**
     * Gets if sync should be paused when the device is in power save mode (battery saver)
     */
    fun monitorPauseSyncOnBatterySaver(): Flow<Boolean?>

    suspend fun setUserPausedSync(syncId: Long)

    suspend fun deleteUserPausedSync(syncId: Long)

    suspend fun isSyncPausedByTheUser(syncId: Long): Boolean

    suspend fun setSyncFrequencyInMinutes(frequencyInMinutes: Int)

    suspend fun getSyncFrequencyMinutes(): Int

    suspend fun setShouldRunForeground(shouldRun: Boolean)

    suspend fun getShouldRunForeground(): Boolean
}
