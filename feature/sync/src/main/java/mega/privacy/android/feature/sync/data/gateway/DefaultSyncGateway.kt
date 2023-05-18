package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.feature.sync.data.forEach
import mega.privacy.android.feature.sync.data.mock.MegaApiSyncMock
import mega.privacy.android.feature.sync.data.mock.MegaApiSyncMock.GlobalUpdate
import mega.privacy.android.feature.sync.data.mock.MegaSync
import mega.privacy.android.feature.sync.data.mock.MegaSyncList
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Gateway implementation to access Sync API
 *
 */
internal class DefaultSyncGateway @Inject constructor(
    private val megaApi: MegaApiSyncMock,
) : SyncGateway {

    private val syncState = MutableSharedFlow<FolderPairState>(replay = 1)

    private val syncStateListener = OptionalMegaRequestListenerInterface(
        onRequestFinish = { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                syncState.tryEmit(FolderPairState.RUNNING)
            }
        }
    )

    override suspend fun syncFolderPair(localPath: String, remoteFolderId: Long) {
        megaApi.syncFolder(
            MegaSync.SyncType.TYPE_TWOWAY,
            localPath,
            null,
            remoteFolderId,
            null,
            syncStateListener
        )
    }

    override suspend fun getFolderPairs(): MegaSyncList =
        megaApi.syncs

    override suspend fun removeFolderPairs() {
        megaApi
            .syncs
            .forEach {
                megaApi.removeSync(it.backupId)
            }
    }

    override fun observeSyncState(): Flow<FolderPairState> {
        // syncState will be updated by using megaapi::onSyncStateChanged. This will be implemented
        // later in a separate task
        return syncState.asSharedFlow()
    }

    override fun monitorSync(): Flow<MegaSync> =
        megaApi
            .globalUpdates
            .filterIsInstance<GlobalUpdate.OnGlobalSyncStateChanged>()
            .map { it.megaSync }

    override fun resumeAllSyncs() {
        megaApi.resumeAllSyncs()
    }

    override fun pauseAllSyncs() {
        megaApi.pauseAllSyncs()
    }
}