package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.SdTransferEntity

@Dao
internal interface SdTransferDao {
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SD_TRANSFERS}")
    fun getAllSdTransfers(): Flow<List<SdTransferEntity>>

    @Insert
    suspend fun insertSdTransfer(entity: SdTransferEntity)

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SD_TRANSFERS} WHERE sdtransfertag = :tag")
    suspend fun deleteSdTransferByTag(tag: Int)
}