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

    @Query("SELECT * FROM active_transfers WHERE uniqueId = :uniqueId")
    suspend fun getActiveTransferByUniqueId(uniqueId: Long): ActiveTransferEntity?

    @Query("SELECT * FROM active_transfers WHERE tag = :tag")
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransferEntity?

    @Query("SELECT * FROM active_transfers WHERE transfer_type = :transferType")
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransferEntity>>

    @Query("SELECT * FROM active_transfers WHERE transfer_type = :transferType")
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransferEntity>

    @Query("SELECT * FROM active_transfers ")
    suspend fun getCurrentActiveTransfers(): List<ActiveTransferEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActiveTransfer(entity: ActiveTransferEntity): Long

    @Query(
        "UPDATE active_transfers " +
                "SET tag = :tag, is_finished = :isFinished, is_paused = :isPaused, is_already_downloaded = :isAlreadyTransferred, total_bytes = :totalBytes, is_cancelled = :isCancelled, file_name = :fileName " +
                "WHERE uniqueId = :uniqueId " +
                "AND is_finished = 0"
    )
    suspend fun updateActiveTransferIfNotFinished(
        uniqueId: Long,
        tag: Int,
        isFinished: Boolean,
        isPaused: Boolean,
        totalBytes: Long,
        isAlreadyTransferred: Boolean,
        isCancelled: Boolean,
        fileName: String,
    )

    suspend fun insertOrUpdateActiveTransfer(entity: ActiveTransferEntity) {
        getActiveTransferByUniqueId(entity.uniqueId)?.let {
            updateActiveTransferIfNotFinished(
                uniqueId = entity.uniqueId,
                tag = entity.tag,
                isFinished = entity.isFinished,
                isPaused = entity.isPaused,
                totalBytes = entity.totalBytes,
                isAlreadyTransferred = entity.isAlreadyTransferred,
                isCancelled = entity.isCancelled,
                fileName = entity.fileName
            )
        } ?: insertActiveTransfer(entity)
    }

    /**
     * Transaction to insert or update a list of entities into the database.
     */
    @Transaction
    suspend fun insertOrUpdateActiveTransfers(
        entities: List<ActiveTransferEntity>,
    ) {
        entities.forEach { entity ->
            insertOrUpdateActiveTransfer(entity)
        }
    }

    @Query("DELETE FROM active_transfers WHERE transfer_type = :transferType")
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    @Query("DELETE FROM active_transfers")
    suspend fun deleteAllActiveTransfers()

    @Query("UPDATE active_transfers SET is_finished = 1, is_cancelled = :cancelled WHERE uniqueId IN (:uniqueIds)")
    suspend fun setActiveTransfersAsFinishedByUniqueId(uniqueIds: List<Long>, cancelled: Boolean)
}