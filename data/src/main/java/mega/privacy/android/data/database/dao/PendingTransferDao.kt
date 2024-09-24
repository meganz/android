package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_PENDING_TRANSFER
import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateScanningFoldersData

@Dao
internal interface PendingTransferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePendingTransfers(entities: List<PendingTransferEntity>): List<Long>

    /**
     * Transaction to insert a list of entities but splitting the insert to avoid SQLiteException too many SQL variables
     */
    @Transaction
    suspend fun insertOrUpdatePendingTransfers(
        entities: List<PendingTransferEntity>,
        chunkSize: Int,
    ) {
        entities.chunked(chunkSize).forEach {
            insertOrUpdatePendingTransfers(it)
        }
    }

    @Query("SELECT * FROM $TABLE_PENDING_TRANSFER WHERE transferTag = :tag")
    suspend fun getPendingTransferByTag(tag: Int): PendingTransferEntity?

    @Query("SELECT * FROM $TABLE_PENDING_TRANSFER WHERE transferType = :transferType")
    fun getPendingTransfersByType(transferType: TransferType): Flow<List<PendingTransferEntity>>

    @Query("SELECT * FROM $TABLE_PENDING_TRANSFER WHERE transferType = :transferType AND state = :state")
    fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        state: PendingTransferState,
    ): Flow<List<PendingTransferEntity>>

    /**
     * Update the pending transfer
     *
     * @param updatePendingTransferState
     */
    @Update(entity = PendingTransferEntity::class)
    suspend fun update(updatePendingTransferState: UpdatePendingTransferState)

    /**
     * Update the pending transfer
     *
     * @param updateAlreadyTransferredFilesCount
     */
    @Update(entity = PendingTransferEntity::class)
    suspend fun update(updateAlreadyTransferredFilesCount: UpdateAlreadyTransferredFilesCount)

    /**
     * Update the pending transfer
     *
     * @param updateScanningFoldersData
     */
    @Update(entity = PendingTransferEntity::class)
    suspend fun update(updateScanningFoldersData: UpdateScanningFoldersData)

    /**
     * Update multiple pending transfers in a single transaction
     *
     * @param updatePendingTransferRequests
     */
    @Transaction
    suspend fun updateMultiple(updatePendingTransferRequests: List<UpdatePendingTransferRequest>) {
        for (request in updatePendingTransferRequests) {
            when (request) {
                is UpdateAlreadyTransferredFilesCount -> update(request)
                is UpdatePendingTransferState -> update(request)
                is UpdateScanningFoldersData -> update(request)
            }
        }
    }

    @Query("DELETE FROM $TABLE_PENDING_TRANSFER WHERE transferTag = :transferTag")
    suspend fun deletePendingTransferByTag(transferTag: Int)

    @Query("DELETE FROM $TABLE_PENDING_TRANSFER")
    suspend fun deleteAllPendingTransfers()
}