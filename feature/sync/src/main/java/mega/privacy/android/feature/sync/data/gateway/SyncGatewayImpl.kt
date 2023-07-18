package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.forEach
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import javax.inject.Inject

/**
 * Gateway implementation to access Sync API
 * Note: commented code will be uncommented in a later MR
 *
 */
internal class SyncGatewayImpl @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaApiGateway: MegaApiGateway,
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
//            megaApi.syncFolder(
//                MegaSync.SyncType.TYPE_TWOWAY,
//                localPath,
//                null,
//                remoteFolderId,
//                null,
//                requestListener
//            )
            continuation.invokeOnCancellation {
                megaApi.removeRequestListener(requestListener)
            }
        }

    override suspend fun getFolderPairs(): MegaSyncList =
        TODO()
//        megaApi.syncs

    override suspend fun removeFolderPairs() {
//        megaApi
//            .syncs
//            .forEach {
//                megaApi.removeSync(it.backupId)
//            }
    }

    override suspend fun removeFolderPair(folderPairId: Long) {
//        megaApi.removeSync(folderPairId)
    }

    override fun monitorSync(): Flow<MegaSync> =
        TODO()
//        megaApiGateway
//            .globalUpdates
//            .filterIsInstance<GlobalUpdate.OnGlobalSyncStateChanged>()
//            .map { it.megaSync }

    override fun resumeAllSyncs() {
//        megaApi.resumeAllSyncs()
    }

    override fun pauseAllSyncs() {
//        megaApi.pauseAllSyncs()
    }
}