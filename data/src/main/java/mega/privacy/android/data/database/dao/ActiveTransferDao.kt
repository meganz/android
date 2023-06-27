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

    @Query("SELECT * FROM ActiveTransferEntity WHERE tag = :tag")
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransferEntity?

    @Query("SELECT * FROM ActiveTransferEntity WHERE transferType = :transferType")
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActiveTransfer(entity: ActiveTransferEntity)

    @Query("DELETE FROM ActiveTransferEntity")
    suspend fun deleteAllActiveTransfers()

    @Query("DELETE FROM ActiveTransferEntity WHERE tag = :tag")
    suspend fun deleteActiveTransferByTag(tag: Int)

}