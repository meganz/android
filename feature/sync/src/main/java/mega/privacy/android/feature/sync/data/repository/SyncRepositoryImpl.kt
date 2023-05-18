package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.mapper.FolderPairMapper
import mega.privacy.android.feature.sync.data.mock.MegaSyncList
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

internal class SyncRepositoryImpl @Inject constructor(
    private val syncGateway: SyncGateway,
    private val megaApi: MegaApiGateway,
    private val folderPairMapper: FolderPairMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncRepository {

    override suspend fun setupFolderPair(
        localPath: String,
        remoteFolderId: Long,
    ): Boolean =
        withContext(ioDispatcher) {
            syncGateway.syncFolderPair(localPath, remoteFolderId)
        }

    override suspend fun resumeAllSyncs() {
        withContext(ioDispatcher) {
            syncGateway.resumeAllSyncs()
        }
    }

    override suspend fun pauseAllSyncs() {
        withContext(ioDispatcher) {
            syncGateway.pauseAllSyncs()
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
                    megaApi.getMegaNodeByHandle(folderPairModel.megaHandle)?.name ?: ""

                folderPairMapper(folderPairModel, megaFolderName)
            }

    override suspend fun removeFolderPairs() {
        withContext(ioDispatcher) {
            syncGateway.removeFolderPairs()
        }
    }

    override fun monitorSync(): Flow<FolderPair> =
        syncGateway
            .monitorSync()
            .map { megaSync ->
                val megaFolderName =
                    megaApi.getMegaNodeByHandle(megaSync.megaHandle)?.name ?: ""

                folderPairMapper(megaSync, megaFolderName)
            }
}
