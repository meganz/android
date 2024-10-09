package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity

/**
 * Dao implementation for [MegaDatabaseConstant.TABLE_SYNC_SHOWN_NOTIFICATIONS]
 */
@Dao
interface SyncShownNotificationDao {

    /**
     * Insert sync shown notification
     * @param syncNotification [SyncShownNotificationEntity]
     */
    @Insert
    suspend fun insertSyncNotification(syncNotification: SyncShownNotificationEntity)

    /**
     * Get [SyncShownNotificationEntity] by type (error, stalled issue, low battery, etc)
     * @return list of [SyncShownNotificationEntity]
     */
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_SHOWN_NOTIFICATIONS} WHERE notificationType = :type")
    suspend fun getSyncNotificationByType(type: String): List<SyncShownNotificationEntity>

    /**
     * Remove [SyncShownNotificationEntity] by type (error, stalled issue, low battery, etc)
     */
    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_SHOWN_NOTIFICATIONS} WHERE notificationType = :type")
    suspend fun deleteSyncNotificationByType(type: String)
}
