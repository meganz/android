package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.listener.OptionalMegaListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStats
import javax.inject.Inject

/**
 * Gateway implementation to access Sync API
 * Note: commented code will be uncommented in a later MR
 *
 */
internal class SyncGatewayImpl @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationScope private val appScope: CoroutineScope,
) : SyncGateway {

    private val syncMegaListenerFlow = callbackFlow {
        val listener = OptionalMegaListenerInterface(
            onSyncDeleted = {
                trySend(MegaSyncListenerEvent.OnSyncDeleted(it))
            },
            onSyncStatsUpdated = {
                trySend(MegaSyncListenerEvent.OnSyncStatsUpdated(it))
            }
        )
        megaApi.addListener(listener)
        awaitClose {
            megaApi.removeListener(listener)
        }
    }.shareIn(
        appScope,
        SharingStarted.WhileSubscribed()
    )

    override suspend fun syncFolderPair(
        name: String?,
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
//                name,
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

    override suspend fun removeFolderPair(folderPairId: Long) {
//        megaApi.removeSync(folderPairId)
    }

    override fun monitorOnSyncDeleted(): Flow<MegaSync> =
        syncMegaListenerFlow
            .filterIsInstance<MegaSyncListenerEvent.OnSyncDeleted>()
            .map { it.sync }

    override fun monitorOnSyncStatsUpdated(): Flow<MegaSyncStats> =
        syncMegaListenerFlow
            .filterIsInstance<MegaSyncListenerEvent.OnSyncStatsUpdated>()
            .map { it.syncStats }

    override fun resumeAllSyncs() {
//        megaApi.resumeAllSyncs()
    }

    override fun pauseAllSyncs() {
//        megaApi.pauseAllSyncs()
    }
}