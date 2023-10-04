package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.mapper.FolderPairMapper
import mega.privacy.android.feature.sync.data.mapper.StalledIssuesMapper
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import nz.mega.sdk.MegaSyncList
import javax.inject.Inject

internal class SyncRepositoryImpl @Inject constructor(
    private val syncGateway: SyncGateway,
    private val syncStatsCacheGateway: SyncStatsCacheGateway,
    private val megaApiGateway: MegaApiGateway,
    private val folderPairMapper: FolderPairMapper,
    private val stalledIssuesMapper: StalledIssuesMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncRepository {

    override suspend fun setupFolderPair(
        name: String?,
        localPath: String,
        remoteFolderId: Long,
    ): Boolean =
        withContext(ioDispatcher) {
            syncGateway.syncFolderPair(name, localPath, remoteFolderId)
        }

    override suspend fun pauseSync(folderPairId: Long) {
        withContext(ioDispatcher) {
            syncGateway.pauseSync(folderPairId)
        }
    }

    override suspend fun resumeSync(folderPairId: Long) {
        withContext(ioDispatcher) {
            syncGateway.resumeSync(folderPairId)
        }
    }

    override suspend fun getFolderPairs(): List<FolderPair> =
        withContext(ioDispatcher) {
            syncGateway
                .getFolderPairs()
                .let { mapToDomain(it) }
        }

    private suspend fun mapToDomain(model: MegaSyncList): List<FolderPair> =
        (0 until model.size())
            .map { index ->
                val folderPairModel = model.get(index)
                val megaFolderName =
                    megaApiGateway.getMegaNodeByHandle(folderPairModel.megaHandle)?.name ?: ""
                val syncStats = syncStatsCacheGateway.getSyncStatsById(folderPairModel.backupId)
                folderPairMapper(
                    folderPairModel,
                    megaFolderName,
                    syncStats
                )
            }

    override suspend fun removeFolderPair(folderPairId: Long) {
        withContext(ioDispatcher) {
            syncGateway.removeFolderPair(folderPairId)
        }
    }

    override fun monitorSyncChanges(): Flow<Unit> =
        merge(
            getOnGlobalSyncStateChangedFlow(),
            syncGateway.monitorOnSyncDeleted(),
            syncGateway.monitorOnSyncStatsUpdated()
                .onEach { syncStatsCacheGateway.setSyncStats(it) },
            syncGateway.monitorOnSyncStateChanged()
        ).map { Unit }
            .flowOn(ioDispatcher)

    private fun getOnGlobalSyncStateChangedFlow(): Flow<GlobalUpdate.OnGlobalSyncStateChanged> =
        megaApiGateway
            .globalUpdates
            .filterIsInstance()

    override suspend fun getSyncStalledIssues(): List<StalledIssue> =
        withContext(ioDispatcher) {
            syncGateway.getSyncStalledIssues()?.let { stalledIssuesMapper(it) }.orEmpty()
        }
}