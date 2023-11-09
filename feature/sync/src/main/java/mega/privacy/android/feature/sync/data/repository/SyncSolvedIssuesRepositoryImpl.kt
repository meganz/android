package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mega.privacy.android.feature.sync.data.gateway.SyncSolvedIssuesGateway
import mega.privacy.android.feature.sync.data.mapper.SyncSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Inject

internal class SyncSolvedIssuesRepositoryImpl @Inject constructor(
    private val syncSolvedIssuesGateway: SyncSolvedIssuesGateway,
    private val syncSolvedIssuesMapper: SyncSolvedIssueMapper,
) : SyncSolvedIssuesRepository {

    private var solvedIssuesCountChangeFlow = MutableSharedFlow<Unit>(replay = 1)

    override suspend fun getAll(): List<SolvedIssue> =
        syncSolvedIssuesGateway
            .getAll()
            .map { syncSolvedIssuesMapper(it) }

    override suspend fun set(solvedIssues: SolvedIssue) {
        syncSolvedIssuesMapper(solvedIssues)
            .let {
                syncSolvedIssuesGateway.set(it)
                solvedIssuesCountChangeFlow.emit(Unit)
            }
    }

    override suspend fun clear() {
        syncSolvedIssuesGateway.clear()
        solvedIssuesCountChangeFlow.emit(Unit)
    }

    override fun monitorSolvedIssuesCountChanged(): Flow<Unit> =
        solvedIssuesCountChangeFlow.asSharedFlow()
}