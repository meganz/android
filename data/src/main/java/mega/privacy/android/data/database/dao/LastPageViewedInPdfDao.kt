package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.entity.LastPageViewedInPdfEntity

@Dao
internal interface LastPageViewedInPdfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLastPageViewedInPdf(entity: LastPageViewedInPdfEntity)

    @Query("SELECT * FROM last_page_viewed_in_pdf WHERE nodeHandle = :handle")
    suspend fun getLastPageViewedInPdfByHandle(handle: Long): LastPageViewedInPdfEntity?

    @Query("DELETE FROM last_page_viewed_in_pdf WHERE nodeHandle = :handle")
    suspend fun deleteLastPageViewedInPdfByHandle(handle: Long)
}