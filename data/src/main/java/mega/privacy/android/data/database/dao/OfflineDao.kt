package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.OfflineEntity

@Dao
internal interface OfflineDao {
    @Query("SELECT * FROM offline")
    fun getAllOffline(): Flow<List<OfflineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOffline(entity: OfflineEntity)

    @Query("SELECT * FROM offline WHERE handle = :handle")
    suspend fun getOfflineByHandle(handle: String?): OfflineEntity?

    @Query("DELETE FROM offline")
    suspend fun deleteAllOffline()

    @Query("SELECT * FROM offline where path = :path")
    fun getOfflineByPath(path: String): Flow<List<OfflineEntity>>

    @Query("SELECT * FROM offline where name = :name")
    fun getOfflineByName(name: String): Flow<List<OfflineEntity>>

    @Query("SELECT * FROM offline where path = :path AND name = :name")
    fun getOfflineByNameAndPath(path: String, name: String): Flow<List<OfflineEntity>>
}