package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue

internal interface SyncSolvedIssuesRepository {

    fun monitorSolvedIssues(): Flow<List<SolvedIssue>>

    suspend fun insertSolvedIssues(solvedIssues: SolvedIssue)

    suspend fun clear()
}