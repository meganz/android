package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity

/**
 * Dao implementation for [MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES]
 */
@Dao
interface SyncSolvedIssuesDao {

    /**
     * Insert solved issues
     * @param solvedIssue [SyncSolvedIssueEntity]
     */
    @Insert
    suspend fun insertSolvedIssue(solvedIssue: SyncSolvedIssueEntity)

    /**
     * monitor changes in [MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES]
     * @return flow of list of [SyncSolvedIssueEntity]
     */
    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES}")
    fun monitorSolvedIssues(): Flow<List<SyncSolvedIssueEntity>>

    /**
     * Removed all [SyncSolvedIssueEntity]
     */
    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES}")
    suspend fun deleteAllSolvedIssues()
}