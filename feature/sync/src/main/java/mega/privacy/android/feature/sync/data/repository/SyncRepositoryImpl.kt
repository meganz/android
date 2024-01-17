package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.mapper.FolderPairMapper
import mega.privacy.android.feature.sync.data.mapper.stalledissue.StalledIssuesMapper
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
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
    @ApplicationScope private val appScope: CoroutineScope,
) : SyncRepository {

    private val _refreshShow = MutableSharedFlow<Unit>()

    override suspend fun setupFolderPair(
        name: String?,
        localPath: String,
        remoteFolderId: Long,
    ): Boolean = withContext(ioDispatcher) {
        syncGateway.syncFolderPair(name, localPath, remoteFolderId)
    }

    override suspend fun pauseSync(folderPairId: Long) = withContext(ioDispatcher) {
        syncGateway.pauseSync(folderPairId)
    }

    override suspend fun resumeSync(folderPairId: Long) = withContext(ioDispatcher) {
        syncGateway.resumeSync(folderPairId)
    }

    override suspend fun getFolderPairs(): List<FolderPair> = withContext(ioDispatcher) {
        mapToDomain(syncGateway.getFolderPairs())
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

    override suspend fun removeFolderPair(folderPairId: Long) = withContext(ioDispatcher) {
        syncGateway.removeFolderPair(folderPairId)
    }


    private val _syncChanges by lazy {
        merge(
            megaApiGateway
                .globalUpdates.filter { it is GlobalUpdate.OnGlobalSyncStateChanged }
                .map { MegaSyncListenerEvent.OnGlobalSyncStateChanged },
            syncGateway.syncUpdate
                .onEach {
                    if (it is MegaSyncListenerEvent.OnSyncStatsUpdated) {
                        syncStatsCacheGateway.setSyncStats(it.syncStats)
                    }
                },
            _refreshShow.map {
                MegaSyncListenerEvent.OnRefreshSyncState
            }
        ).flowOn(ioDispatcher)
            .shareIn(appScope, SharingStarted.Eagerly)
    }
    override val syncChanges: Flow<MegaSyncListenerEvent> = _syncChanges

    override suspend fun getSyncStalledIssues(): List<StalledIssue> = withContext(ioDispatcher) {
        syncGateway.getSyncStalledIssues()?.let { stalledIssuesMapper(it) }.orEmpty()
    }

    private val _syncStalledIssues by lazy {
        _syncChanges
            .map { getSyncStalledIssues() }
            .onStart { emit(getSyncStalledIssues()) }
            .flowOn(ioDispatcher)
            .shareIn(appScope, SharingStarted.Eagerly, replay = 1)
    }

    override fun monitorStalledIssues() = _syncStalledIssues

    private val _folderPair by lazy {
        _syncChanges
            .map { getFolderPairs() }
            .onStart { emit(getFolderPairs()) }
            .flowOn(ioDispatcher)
            .shareIn(appScope, SharingStarted.Eagerly, replay = 1)
    }

    override fun monitorFolderPairChanges() = _folderPair

    override suspend fun refreshSync() {
        _refreshShow.emit(Unit)
    }
}