package mega.privacy.android.feature.sync.data.gateway

import dagger.Lazy
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import javax.inject.Inject

internal class SyncSolvedIssuesGatewayImpl @Inject constructor(
    private val syncSolvedIssuesDao: Lazy<SyncSolvedIssuesDao>,
) : SyncSolvedIssuesGateway {

    override suspend fun set(solvedIssue: SyncSolvedIssueEntity) {
        syncSolvedIssuesDao.get().insertSolvedIssue(solvedIssue)
    }

    override fun monitorSyncSolvedIssues() =
        syncSolvedIssuesDao.get().monitorSolvedIssues()

    override suspend fun clear() {
        syncSolvedIssuesDao.get().deleteAllSolvedIssues()
    }

    override suspend fun removeBySyncId(syncId: Long) {
        syncSolvedIssuesDao.get().removeBySyncId(syncId)
    }
}