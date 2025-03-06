package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_ACTIVE_TRANSFER_ACTION_GROUPS
import mega.privacy.android.data.database.entity.ActiveTransferActionGroupEntity

@Dao
internal interface ActiveTransferGroupDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActiveTransferGroup(entity: ActiveTransferActionGroupEntity): Long

    @Query("SELECT * FROM $TABLE_ACTIVE_TRANSFER_ACTION_GROUPS WHERE group_id = :id")
    suspend fun getActiveTransferGroupById(id: Int): ActiveTransferActionGroupEntity?

    @Query("DELETE FROM $TABLE_ACTIVE_TRANSFER_ACTION_GROUPS WHERE group_id = :groupId")
    suspend fun deleteActiveTransfersGroupById(groupId: Int)
}