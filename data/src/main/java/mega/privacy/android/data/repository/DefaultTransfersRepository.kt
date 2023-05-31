package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.isBackgroundTransfer
import mega.privacy.android.data.extensions.transferListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.TransferDataMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.TransferRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.COLLISION_CHECK_FINGERPRINT
import nz.mega.sdk.MegaTransfer.COLLISION_RESOLUTION_NEW_WITH_N
import javax.inject.Inject

/**
 * Default [TransferRepository] implementation.
 *
 * @param megaApiGateway    [MegaApiGateway]
 * @param ioDispatcher      [IoDispatcher]
 * @param transferEventMapper [TransferEventMapper]
 * @param transferMapper [TransferEventMapper]
 * @param appEventGateway [AppEventGateway]
 * @param localStorageGateway [MegaLocalStorageGateway]
 */
internal class DefaultTransfersRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val transferEventMapper: TransferEventMapper,
    private val transferMapper: TransferMapper,
    private val appEventGateway: AppEventGateway,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val workerManagerGateway: WorkManagerGateway,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val transferDataMapper: TransferDataMapper,
) : TransfersRepository, TransferRepository {

    override suspend fun cancelTransfer(transfer: MegaTransfer) {
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("cancelTransfer") {}
                megaApiGateway.cancelTransfer(
                    transfer = transfer,
                    listener = listener
                )
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
    }

    override fun startUpload(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        cancelToken: MegaCancelToken?,
    ): Flow<GlobalTransfer> = callbackFlow {
        val parentNode = megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
        requireNotNull(parentNode)
        val listener = uploadListener(channel)

        megaApiGateway.startUpload(
            localPath = localPath,
            parentNode = parentNode,
            fileName = fileName,
            modificationTime = modificationTime,
            appData = appData,
            isSourceTemporary = isSourceTemporary,
            shouldStartFirst = shouldStartFirst,
            cancelToken = cancelToken,
            listener = listener,
        )

        awaitClose {
            cancelToken?.cancel()
            megaApiGateway.removeTransferListener(listener)
        }
    }
        .flowOn(ioDispatcher)
        .cancellable()

    private fun uploadListener(
        channel: SendChannel<GlobalTransfer>,
    ) = OptionalMegaTransferListenerInterface(
        onTransferStart = { transfer ->
            channel.trySend(GlobalTransfer.OnTransferStart(transfer))
        },
        onTransferFinish = { transfer, error ->
            channel.trySend(GlobalTransfer.OnTransferFinish(transfer, error))
            channel.close()
        },
        onTransferUpdate = { transfer ->
            channel.trySend(GlobalTransfer.OnTransferUpdate(transfer))
        },
        onTransferTemporaryError = { transfer, error ->
            channel.trySend(GlobalTransfer.OnTransferTemporaryError(transfer, error))
        },
        onTransferData = { transfer, buffer ->
            channel.trySend(GlobalTransfer.OnTransferData(transfer, buffer))
        },
    )

    override fun startDownload(
        nodeId: NodeId,
        localPath: String,
        appData: String?,
        shouldStartFirst: Boolean,
    ) = callbackFlow {
        runCatching {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
        }.getOrNull()?.let { megaNode ->
            val cancelToken = MegaCancelToken.createInstance()
            val listener = this.transferListener("Download node") {
                transferMapper(it)
            }
            megaApiGateway.startDownload(
                node = megaNode,
                localPath = localPath,
                fileName = megaNode.name,
                appData = appData,
                startFirst = shouldStartFirst,
                cancelToken = cancelToken,
                collisionCheck = COLLISION_CHECK_FINGERPRINT,
                collisionResolution = COLLISION_RESOLUTION_NEW_WITH_N,
                listener = listener,
            )

            awaitClose {
                cancelToken?.cancel()
                megaApiGateway.removeTransferListener(listener)
            }
        }
    }
        .flowOn(ioDispatcher)
        .cancellable()

    override suspend fun getTransferData() = withContext(ioDispatcher) {
        megaApiGateway.getTransferData()?.let { transferDataMapper(it) }
    }


    override suspend fun cancelAllDownloadTransfers() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("cancelAllDownloadTransfers") {}
            megaApiGateway.cancelAllDownloadTransfers(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun cancelAllUploadTransfers() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("cancelAllUploadTransfers") {}
            megaApiGateway.cancelAllUploadTransfers(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    private suspend fun getUploadTransfers(): List<MegaTransfer> = withContext(ioDispatcher) {
        megaApiGateway.getTransfers(MegaTransfer.TYPE_UPLOAD)
    }

    private suspend fun getDownloadTransfers(): List<MegaTransfer> = withContext(ioDispatcher) {
        megaApiGateway.getTransfers(MegaTransfer.TYPE_DOWNLOAD)
    }

    override suspend fun getNumPendingDownloadsNonBackground(): Int = withContext(ioDispatcher) {
        getDownloadTransfers().count { transfer ->
            !transfer.isFinished && !transfer.isBackgroundTransfer()
        }
    }

    override suspend fun getNumPendingUploads(): Int = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer -> !transfer.isFinished }
    }

    override suspend fun getNumPendingTransfers(): Int = withContext(ioDispatcher) {
        getNumPendingDownloadsNonBackground() + getNumPendingUploads()
    }

    override suspend fun isCompletedTransfersEmpty(): Boolean = withContext(ioDispatcher) {
        megaLocalRoomGateway.getCompletedTransfersCount() == 0
    }

    override suspend fun areTransfersPaused(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.areUploadTransfersPaused() || megaApiGateway.areDownloadTransfersPaused()
    }

    override suspend fun getNumPendingPausedUploads(): Int = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.state == MegaTransfer.STATE_PAUSED
        }
    }

    override suspend fun getNumPendingNonBackgroundPausedDownloads(): Int =
        withContext(ioDispatcher) {
            getDownloadTransfers().count { transfer ->
                !transfer.isFinished && !transfer.isBackgroundTransfer() && transfer.state == MegaTransfer.STATE_PAUSED
            }
        }

    override suspend fun areAllTransfersPaused(): Boolean = withContext(ioDispatcher) {
        areTransfersPaused() || getNumPendingPausedUploads() + getNumPendingNonBackgroundPausedDownloads() == getNumPendingTransfers()
    }

    override fun monitorTransferEvents(): Flow<TransferEvent> =
        megaApiGateway.globalTransfer.map { event -> transferEventMapper(event) }
            .flowOn(ioDispatcher)

    override suspend fun cancelTransferByTag(transferTag: Int) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.failWithError(error, "cancelTransferByTag")
                    }
                }
            )
            megaApiGateway.cancelTransferByTag(
                transferTag = transferTag,
                listener = listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun resetTotalDownloads() = withContext(ioDispatcher) {
        megaApiGateway.resetTotalDownloads()
    }

    override fun monitorOfflineFileAvailability(): Flow<Long> =
        appEventGateway.monitorOfflineFileAvailability()


    override suspend fun broadcastOfflineFileAvailability(nodeHandle: Long) {
        appEventGateway.broadcastOfflineFileAvailability(nodeHandle)
    }

    override fun monitorTransferOverQuota(): Flow<Boolean> =
        appEventGateway.monitorTransferOverQuota()

    override suspend fun broadcastTransferOverQuota() {
        appEventGateway.broadcastTransferOverQuota()
    }

    override suspend fun cancelTransfers() = withContext(ioDispatcher) {
        megaApiGateway.cancelTransfers(MegaTransfer.TYPE_UPLOAD)
        megaApiGateway.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD)
    }

    override fun monitorFailedTransfer(): Flow<Boolean> = appEventGateway.monitorFailedTransfer()

    override suspend fun broadcastFailedTransfer(isFailed: Boolean) {
        appEventGateway.broadcastFailedTransfer(isFailed)
    }

    override suspend fun ongoingTransfersExist(): Boolean =
        megaApiGateway.numberOfPendingUploads > 0 || megaApiGateway.numberOfPendingDownloads > 0

    override suspend fun moveTransferToFirstByTag(transferTag: Int) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("moveTransferToFirstByTag") { }
            megaApiGateway.moveTransferToFirstByTag(transferTag, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun moveTransferToLastByTag(transferTag: Int) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("moveTransferToLastByTag") { }
            megaApiGateway.moveTransferToLastByTag(transferTag, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun moveTransferBeforeByTag(transferTag: Int, prevTransferTag: Int) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("moveTransferBeforeByTag") { }
                megaApiGateway.moveTransferBeforeByTag(transferTag, prevTransferTag, listener)
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }

    override suspend fun getTransferByTag(transferTag: Int) = withContext(ioDispatcher) {
        megaApiGateway.getTransfersByTag(transferTag)?.let { transferMapper(it) }
    }

    override fun monitorPausedTransfers() = appEventGateway.monitorPausedTransfers()

    override suspend fun broadcastPausedTransfers() = appEventGateway.broadcastPausedTransfers()

    override suspend fun getInProgressTransfers(): List<Transfer> = withContext(ioDispatcher) {
        val transfers = mutableListOf<Transfer>()
        megaApiGateway.getTransferData()?.let { data ->
            transfers.addAll((0 until data.numDownloads)
                .mapNotNull { getTransferByTag(data.getDownloadTag(it)) })
            transfers.addAll((0 until data.numUploads)
                .mapNotNull { getTransferByTag(data.getUploadTag(it)) })
        }
        transfers.sortedBy { it.priority }
    }

    override fun monitorCompletedTransfer(): Flow<CompletedTransfer> =
        appEventGateway.monitorCompletedTransfer

    override suspend fun getAllCompletedTransfers(size: Int?): Flow<List<CompletedTransfer>> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getAllCompletedTransfers(size)
        }

    override suspend fun addCompletedTransfer(transfer: CompletedTransfer) =
        withContext(ioDispatcher) {
            localStorageGateway.addCompletedTransfer(transfer)
            appEventGateway.broadcastCompletedTransfer(transfer)
        }

    override suspend fun deleteOldestCompletedTransfers() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueDeleteOldestCompletedTransfersWorkRequest()
    }

    override fun monitorTransfersFinished() = appEventGateway.monitorTransfersFinished()

    override suspend fun broadcastTransfersFinished(transfersFinishedState: TransfersFinishedState) =
        appEventGateway.broadcastTransfersFinished(transfersFinishedState)

    override fun monitorStopTransfersWork() = appEventGateway.monitorStopTransfersWork()

    override suspend fun broadcastStopTransfersWork() = appEventGateway.broadcastStopTransfersWork()

    override suspend fun resetTotalUploads() = withContext(ioDispatcher) {
        megaApiGateway.resetTotalUploads()
    }
}
