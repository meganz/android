package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity

internal interface SyncSolvedIssuesGateway {
    suspend fun set(solvedIssue: SyncSolvedIssueEntity)

    fun monitorSyncSolvedIssues(): Flow<List<SyncSolvedIssueEntity>>

    suspend fun clear()
}