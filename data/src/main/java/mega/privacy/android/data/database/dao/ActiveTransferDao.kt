package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActiveTransfers(entities: List<ActiveTransferEntity>)

    /**
     * Transaction to insert a list of entities but splitting the insert to avoid SQLiteException too many SQL variables
     */
    @Transaction
    suspend fun insertOrUpdateActiveTransfers(
        entities: List<ActiveTransferEntity>,
        chunkSize: Int,
    ) {
        entities.chunked(chunkSize).forEach {
            insertOrUpdateActiveTransfers(it)
        }
    }

    @Query("DELETE FROM active_transfers WHERE transfer_type = :transferType")
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    @Query("UPDATE active_transfers SET is_finished = 1 WHERE tag IN (:tags)")
    suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>)
}