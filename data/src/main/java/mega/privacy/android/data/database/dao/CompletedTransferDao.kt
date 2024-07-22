package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.CompletedTransferEntity

@Dao
internal interface CompletedTransferDao {
    @Query("SELECT * FROM completedtransfers")
    fun getAllCompletedTransfers(): Flow<List<CompletedTransferEntity>>

    @Query("SELECT * FROM completedtransfers WHERE transferstate IN(:states)")
    fun getCompletedTransfersByState(states: List<String>): List<CompletedTransferEntity>

    @Query("SELECT * FROM completedtransfers WHERE id = :id")
    suspend fun getCompletedTransferById(id: Int): CompletedTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletedTransfer(entity: CompletedTransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletedTransfers(entities: List<CompletedTransferEntity>)

    /**
     * Transaction to insert a list of entities but splitting the insert to avoid SQLiteException too many SQL variables
     */
    @Transaction
    suspend fun insertOrUpdateCompletedTransfers(
        entities: List<CompletedTransferEntity>,
        chunkSize: Int,
    ) {
        entities.chunked(chunkSize).forEach {
            insertOrUpdateCompletedTransfers(it)
        }
    }

    @Query("DELETE FROM completedtransfers")
    suspend fun deleteAllCompletedTransfers()

    @Query("DELETE FROM completedtransfers WHERE id IN(:ids)")
    suspend fun deleteCompletedTransferByIds(ids: List<Int>)

    /**
     * Transaction to delete a list of entities with their IDs but splitting the delete to avoid SQLiteException too many SQL variables
     */
    @Transaction
    suspend fun deleteCompletedTransferByIds(ids: List<Int>, chunkSize: Int) {
        ids.chunked(chunkSize).forEach {
            deleteCompletedTransferByIds(it)
        }
    }

    @Query("SELECT COUNT(id) FROM completedtransfers")
    suspend fun getCompletedTransfersCount(): Int
}
