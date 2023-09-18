package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.TransferType

@Dao
internal interface ActiveTransferDao {

    @Query("SELECT * FROM active_transfers WHERE tag = :tag")
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransferEntity?

    @Query("SELECT * FROM active_transfers WHERE transfer_type = :transferType")
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransferEntity>>

    @Query("SELECT * FROM active_transfers WHERE transfer_type = :transferType")
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActiveTransfer(entity: ActiveTransferEntity)

    @Query("DELETE FROM active_transfers WHERE transfer_type = :transferType")
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    @Query("UPDATE active_transfers SET is_finished = 1 WHERE tag IN (:tags)")
    suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>)
}