package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.flow.map
import mega.privacy.android.feature.sync.data.gateway.SyncSolvedIssuesGateway
import mega.privacy.android.feature.sync.data.mapper.SyncSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

internal class SyncSolvedIssuesRepositoryImpl @Inject constructor(
    private val syncSolvedIssuesGateway: SyncSolvedIssuesGateway,
    private val syncSolvedIssuesMapper: SyncSolvedIssueMapper,
) : SyncSolvedIssuesRepository {

    override fun monitorSolvedIssues() =
        syncSolvedIssuesGateway.monitorSyncSolvedIssues().map {
            it.map { entity ->
                syncSolvedIssuesMapper(entity)
            }
        }

    override suspend fun insertSolvedIssues(solvedIssues: SolvedIssue) {
        syncSolvedIssuesMapper(solvedIssues)
            .let {
                syncSolvedIssuesGateway.set(it)
            }
    }

    override suspend fun clear() {
        syncSolvedIssuesGateway.clear()
    }
}