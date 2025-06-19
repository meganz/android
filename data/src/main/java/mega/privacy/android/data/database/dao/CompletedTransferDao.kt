package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_COMPLETED_TRANSFERS
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_COMPLETED_TRANSFERS_LEGACY
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntityLegacy

@Dao
internal interface CompletedTransferDao {
    @Query("SELECT * FROM $TABLE_COMPLETED_TRANSFERS")
    fun getAllCompletedTransfers(): Flow<List<CompletedTransferEntity>>

    @Query("SELECT * FROM $TABLE_COMPLETED_TRANSFERS WHERE transferstate IN(:states)")
    fun getCompletedTransfersByState(states: List<Int>): List<CompletedTransferEntity>

    @Query("SELECT * FROM $TABLE_COMPLETED_TRANSFERS WHERE id = :id")
    suspend fun getCompletedTransferById(id: Int): CompletedTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletedTransfer(entity: CompletedTransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletedTransfers(entities: List<CompletedTransferEntity>)

    @Query("DELETE FROM $TABLE_COMPLETED_TRANSFERS WHERE transferstate = :transferState AND id NOT IN (SELECT id FROM $TABLE_COMPLETED_TRANSFERS WHERE transferstate = :transferState ORDER BY transfertimestamp DESC LIMIT :limit)")
    suspend fun deleteOldCompletedTransfersByState(transferState: Int, limit: Int)

    /**
     * Inserts or updates a list of completed transfer entities in chunks to avoid SQLite variable limits,
     * and prunes old entries to keep only the [maxPerState] most recent transfers per state.
     *
     * @param entities The list of completed transfer entities to insert or update.
     * @param maxPerState The maximum number of entities of each state.
     * @param chunkSize The maximum number of entities to insert in a single chunk.
     */
    @Transaction
    suspend fun insertOrUpdateAndPruneCompletedTransfers(
        entities: List<CompletedTransferEntity>,
        maxPerState: Int,
        chunkSize: Int = maxPerState,
    ) {
        entities.chunked(chunkSize).forEach {
            insertOrUpdateCompletedTransfers(it)
        }
        entities.map { it.state }.distinct().forEach {
            deleteOldCompletedTransfersByState(it, maxPerState)
        }
    }

    @Query("DELETE FROM $TABLE_COMPLETED_TRANSFERS")
    suspend fun deleteAllCompletedTransfers()

    @Query("DELETE FROM $TABLE_COMPLETED_TRANSFERS_LEGACY")
    suspend fun deleteAllLegacyCompletedTransfers()

    @Query("DELETE FROM $TABLE_COMPLETED_TRANSFERS WHERE id IN(:ids)")
    suspend fun deleteCompletedTransferByIds(ids: List<Int>)

    @Query("DELETE FROM $TABLE_COMPLETED_TRANSFERS WHERE transferpath = :path")
    suspend fun deleteCompletedTransfersByPath(path: String)

    /**
     * Transaction to delete a list of entities with their IDs but splitting the delete to avoid SQLiteException too many SQL variables
     */
    @Transaction
    suspend fun deleteCompletedTransferByIds(ids: List<Int>, chunkSize: Int) {
        ids.chunked(chunkSize).forEach {
            deleteCompletedTransferByIds(it)
        }
    }

    @Query("SELECT COUNT(id) FROM $TABLE_COMPLETED_TRANSFERS")
    suspend fun getCompletedTransfersCount(): Int

    @Query("SELECT * FROM $TABLE_COMPLETED_TRANSFERS_LEGACY")
    suspend fun getAllLegacyCompletedTransfers(): List<CompletedTransferEntityLegacy>
}
