package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.UserPausedSyncEntity

/**
 * Dao implementation for [MegaDatabaseConstant.TABLE_USER_PAUSED_SYNCS]
 *
 * This table is used to store the syncs paused manually by the user
 */
@Dao
interface UserPausedSyncsDao {

    /**
     * Insert paused sync
     */
    @Insert
    suspend fun insertPausedSync(pausedSync: UserPausedSyncEntity)

    /**
     * Get paused sync
     */
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_USER_PAUSED_SYNCS} WHERE sync_id = :syncId")
    suspend fun getUserPausedSync(syncId: Long): UserPausedSyncEntity?

    /**
     * Delete paused syncs
     */
    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_USER_PAUSED_SYNCS} WHERE sync_id = :syncId")
    suspend fun deleteUserPausedSync(syncId: Long)
}