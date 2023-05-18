package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.feature.sync.data.forEach
import mega.privacy.android.feature.sync.data.mock.MegaApiSyncMock
import mega.privacy.android.feature.sync.data.mock.MegaApiSyncMock.GlobalUpdate
import mega.privacy.android.feature.sync.data.mock.MegaSync
import mega.privacy.android.feature.sync.data.mock.MegaSyncList
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Gateway implementation to access Sync API
 *
 */
internal class SyncGatewayImpl @Inject constructor(
    private val megaApi: MegaApiSyncMock,
) : SyncGateway {

    override suspend fun syncFolderPair(
        localPath: String,
        remoteFolderId: Long,
    ): Boolean =
        suspendCancellableCoroutine { continuation ->
            val requestListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(true))
                    } else {
                        continuation.resumeWith(Result.success(false))
                    }
                }
            )
            megaApi.syncFolder(
                MegaSync.SyncType.TYPE_TWOWAY,
                localPath,
                null,
                remoteFolderId,
                null,
                requestListener
            )
            continuation.invokeOnCancellation {
                megaApi.removeRequestListener(requestListener)
            }
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