package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED_TYPE
import mega.privacy.android.data.database.entity.RecentlyUsedTypeEntity

/**
 * DAO for the recently used type lookup table.
 */
@Dao
internal interface RecentlyUsedTypeDao {
    /**
     * Get all recently used types.
     */
    @Query("SELECT * FROM $TABLE_RECENTLY_USED_TYPE")
    suspend fun getAll(): List<RecentlyUsedTypeEntity>

    /**
     * Insert all types, ignoring conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<RecentlyUsedTypeEntity>)
}
