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

    @Query("SELECT * FROM active_transfers ")
    suspend fun getCurrentActiveTransfers(): List<ActiveTransferEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActiveTransfer(entity: ActiveTransferEntity): Long

    @Query(
        "UPDATE active_transfers " +
                "SET is_finished = :isFinished, is_paused = :isPaused, is_already_downloaded = :isAlreadyTransferred, total_bytes = :totalBytes, is_cancelled = :isCancelled, file_name = :fileName " +
                "WHERE tag = :tag " +
                "AND is_finished = 0"
    )
    suspend fun updateActiveTransferIfNotFinished(
        tag: Int,
        isFinished: Boolean,
        isPaused: Boolean,
        totalBytes: Long,
        isAlreadyTransferred: Boolean,
        isCancelled: Boolean,
        fileName: String,
    )

    @Transaction
    suspend fun insertOrUpdateActiveTransfer(entity: ActiveTransferEntity) {
        val id = insertActiveTransfer(entity)
        if (id == -1L) {
            updateActiveTransferIfNotFinished(
                tag = entity.tag,
                isFinished = entity.isFinished,
                isPaused = entity.isPaused,
                totalBytes = entity.totalBytes,
                isAlreadyTransferred = entity.isAlreadyTransferred,
                isCancelled = entity.isCancelled,
                fileName = entity.fileName
            )
        }
    }

    /**
     * Transaction to insert a list of entities but splitting the insert to avoid SQLiteException too many SQL variables
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

    @Query("UPDATE active_transfers SET is_finished = 1, is_cancelled = 1 WHERE tag IN (:tags)")
    suspend fun setActiveTransferAsCancelledByTag(tags: List<Int>)
}