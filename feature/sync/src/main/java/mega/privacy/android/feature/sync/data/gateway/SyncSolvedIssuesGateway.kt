package mega.privacy.android.feature.sync.data.gateway

import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity

internal interface SyncSolvedIssuesGateway {

    suspend fun getAll(): List<SyncSolvedIssueEntity>

    suspend fun set(solvedIssue: SyncSolvedIssueEntity)

    suspend fun clear()
}