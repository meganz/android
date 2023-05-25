package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.CompletedTransferEntity

@Dao
internal interface CompletedTransferDao {
    @Query("SELECT * FROM completedtransfers")
    fun getAllCompletedTransfers(): Flow<List<CompletedTransferEntity>>

    @Query("SELECT * FROM completedtransfers WHERE id = :id")
    suspend fun getCompletedTransferById(id: Int): CompletedTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletedTransfer(entity: CompletedTransferEntity)

    @Query("DELETE FROM completedtransfers")
    suspend fun deleteAllCompletedTransfers()

    @Query("DELETE FROM completedtransfers WHERE id = :id")
    suspend fun deleteCompletedTransferById(id: String)

    @Query("SELECT COUNT(id) FROM completedtransfers")
    suspend fun getCompletedTransfersCount(): Int
}
