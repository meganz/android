package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue

internal interface SyncSolvedIssuesRepository {

    suspend fun getAll(): List<SolvedIssue>

    suspend fun set(solvedIssues: SolvedIssue)

    suspend fun clear()

    fun monitorSolvedIssuesCountChanged(): Flow<Unit>
}