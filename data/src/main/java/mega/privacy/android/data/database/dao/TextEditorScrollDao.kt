package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_TEXT_EDITOR_SCROLL
import mega.privacy.android.data.database.entity.TextEditorScrollEntity

/**
 * DAO for the text editor scroll state table.
 */
@Dao
internal interface TextEditorScrollDao {
    /**
     * Insert or update text editor scroll state.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: TextEditorScrollEntity)

    /**
     * Get text editor scroll state by node handle.
     */
    @Query("SELECT * FROM $TABLE_TEXT_EDITOR_SCROLL WHERE node_handle = :nodeHandle")
    suspend fun getByNodeHandle(nodeHandle: Long): TextEditorScrollEntity?

    /**
     * Delete text editor scroll state by node handle.
     */
    @Query("DELETE FROM $TABLE_TEXT_EDITOR_SCROLL WHERE node_handle = :nodeHandle")
    suspend fun deleteByNodeHandle(nodeHandle: Long)

    /**
     * Delete all text editor scroll states.
     */
    @Query("DELETE FROM $TABLE_TEXT_EDITOR_SCROLL")
    suspend fun deleteAll()
}
