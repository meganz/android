package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_FOLDER_PREFERENCE
import mega.privacy.android.data.database.entity.FolderPreferenceEntity

/**
 * DAO for per-folder UI preferences (sort order and view mode).
 */
@Dao
internal interface FolderPreferenceDao {

    /**
     * Insert or replace the preferences for a folder.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: FolderPreferenceEntity)

    /**
     * Monitor the stored preferences for a folder.
     *
     * @return a [Flow] emitting the folder's [FolderPreferenceEntity], or null if none is stored.
     */
    @Query("SELECT * FROM $TABLE_FOLDER_PREFERENCE WHERE folder_key = :folderKey")
    fun monitorByFolderKey(folderKey: String): Flow<FolderPreferenceEntity?>

    /**
     * Delete all stored folder preferences (e.g. on logout).
     */
    @Query("DELETE FROM $TABLE_FOLDER_PREFERENCE")
    suspend fun deleteAll()
}
