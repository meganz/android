package mega.privacy.android.feature.sync.data.gateway

import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import javax.inject.Inject

internal class SyncSolvedIssuesGatewayImpl @Inject constructor(
    private val syncSolvedIssuesDao: SyncSolvedIssuesDao,
) : SyncSolvedIssuesGateway {

    override suspend fun set(solvedIssue: SyncSolvedIssueEntity) {
        syncSolvedIssuesDao.insertSolvedIssue(solvedIssue)
    }

    override fun monitorSyncSolvedIssues() =
        syncSolvedIssuesDao.monitorSolvedIssues()

    override suspend fun clear() {
        syncSolvedIssuesDao.deleteAllSolvedIssues()
    }
}