package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_ACTIVE_TRANSFER_GROUPS
import mega.privacy.android.data.database.entity.ActiveTransferGroupEntity

@Dao
internal interface ActiveTransferGroupDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActiveTransferGroup(entity: ActiveTransferGroupEntity): Long

    @Query("SELECT * FROM $TABLE_ACTIVE_TRANSFER_GROUPS WHERE group_id = :id")
    suspend fun getActiveTransferGroupById(id: Int): ActiveTransferGroupEntity?

    @Query("DELETE FROM $TABLE_ACTIVE_TRANSFER_GROUPS WHERE group_id = :groupId")
    suspend fun deleteActiveTransfersGroupById(groupId: Int)
}