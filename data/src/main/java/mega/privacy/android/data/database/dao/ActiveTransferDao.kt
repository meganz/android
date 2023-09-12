package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.ActiveTransferTotalsEntity
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

    @Query(TOTALS_QUERY)
    fun getTotalsByType(transferType: TransferType): Flow<ActiveTransferTotalsEntity>

    @Query(TOTALS_QUERY)
    fun getCurrentTotalsByType(transferType: TransferType): ActiveTransferTotalsEntity

    companion object {
        private const val TOTALS_QUERY =
            "SELECT transfer_type as transfersType, SUM(total_bytes) as totalBytes, SUM(transferred_bytes) as transferredBytes, COUNT(*) as totalTransfers, SUM(is_finished) as totalFinishedTransfers FROM active_transfers WHERE transfer_type = :transferType GROUP by transfer_type"
    }
}