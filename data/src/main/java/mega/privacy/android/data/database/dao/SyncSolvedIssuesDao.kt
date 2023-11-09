package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity

@Dao
interface SyncSolvedIssuesDao {
    @Insert
    suspend fun insertSolvedIssue(solvedIssue: SyncSolvedIssueEntity)

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES}")
    suspend fun getAllSolvedIssues(): List<SyncSolvedIssueEntity>

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_SYNC_SOLVED_ISSUES}")
    suspend fun deleteAllSolvedIssues()
}