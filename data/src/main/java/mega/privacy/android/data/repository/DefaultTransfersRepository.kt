package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.isBackgroundTransfer
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.mapper.transfer.PausedTransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferDataMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferTotalsMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.TransferRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.COLLISION_CHECK_FINGERPRINT
import nz.mega.sdk.MegaTransfer.COLLISION_RESOLUTION_NEW_WITH_N
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
internal class DefaultTransfersRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    private val transferEventMapper: TransferEventMapper,
    private val pausedTransferEventMapper: PausedTransferEventMapper,
    private val transferMapper: TransferMapper,
    private val transferAppDataStringMapper: TransferAppDataStringMapper,
    private val activeTransferTotalsMapper: ActiveTransferTotalsMapper,
    private val appEventGateway: AppEventGateway,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val workerManagerGateway: WorkManagerGateway,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val transferDataMapper: TransferDataMapper,
    private val cancelTokenProvider: CancelTokenProvider,
) : TransferRepository {

    private val monitorPausedTransfers = MutableStateFlow(false)

    /**
     * to store current transferred bytes in memory instead of in database
     */
    private val transferredBytesFlows =
        HashMap<TransferType, MutableStateFlow<Map<Int, Long>>>()

    init {
        //update monitorPausedTransfer with current sdk value
        scope.launch {
            monitorPausedTransfers.emit(megaApiGateway.areUploadTransfersPaused() || megaApiGateway.areDownloadTransfersPaused())
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
    ) = callbackFlow {
        val parentNode = megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
        requireNotNull(parentNode)
        val listener = transferListener(channel)

        megaApiGateway.startUpload(
            localPath = localPath,
            parentNode = parentNode,
            fileName = fileName,
            modificationTime = modificationTime,
            appData = appData,
            isSourceTemporary = isSourceTemporary,
            shouldStartFirst = shouldStartFirst,
            cancelToken = null,
            listener = listener,
        )

        awaitClose {
            megaApiGateway.removeTransferListener(listener)
        }
    }
        .flowOn(ioDispatcher)
        .cancellable()

    private fun transferListener(
        channel: SendChannel<TransferEvent>,
    ) = OptionalMegaTransferListenerInterface(
        onTransferStart = { transfer ->
            channel.trySend(transferEventMapper(GlobalTransfer.OnTransferStart(transfer)))
        },
        onTransferFinish = { transfer, error ->
            channel.trySend(transferEventMapper(GlobalTransfer.OnTransferFinish(transfer, error)))
            channel.close()
        },
        onTransferUpdate = { transfer ->
            channel.trySend(transferEventMapper(GlobalTransfer.OnTransferUpdate(transfer)))
        },
        onTransferTemporaryError = { transfer, error ->
            channel.trySend(
                transferEventMapper(
                    GlobalTransfer.OnTransferTemporaryError(
                        transfer, error
                    )
                )
            )
        },
        onTransferData = { transfer, buffer ->
            channel.trySend(transferEventMapper(GlobalTransfer.OnTransferData(transfer, buffer)))
        },
    )

    override fun startDownload(
        nodeId: NodeId,
        localPath: String,
        appData: TransferAppData?,
        shouldStartFirst: Boolean,
    ) = callbackFlow {
        val listener = transferListener(channel)
        val megaNodeResult = runCatching { megaApiGateway.getMegaNodeByHandle(nodeId.longValue) }
        megaNodeResult.getOrNull()?.let { megaNode ->
            megaApiGateway.startDownload(
                node = megaNode,
                localPath = localPath,
                fileName = megaNode.name,
                appData = appData?.let { transferAppDataStringMapper(listOf(it)) },
                startFirst = shouldStartFirst,
                cancelToken = cancelTokenProvider.getOrCreateCancelToken(),
                collisionCheck = COLLISION_CHECK_FINGERPRINT,
                collisionResolution = COLLISION_RESOLUTION_NEW_WITH_N,
                listener = listener,
            )
        } ?: run {
            throw NodeDoesNotExistsException()
        }
        awaitClose {
            megaApiGateway.removeTransferListener(listener)
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

    override suspend fun getNumPendingGeneralUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && !transfer.isCUUpload() && !transfer.isChatUpload()
        }
    }

    override suspend fun getNumPendingCameraUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.isCUUpload()
        }
    }

    override suspend fun getNumPendingChatUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.isChatUpload()
        }
    }

    override suspend fun getNumPendingTransfers(): Int = withContext(ioDispatcher) {
        getNumPendingDownloadsNonBackground() + getNumPendingUploads()
    }

    override suspend fun isCompletedTransfersEmpty(): Boolean = withContext(ioDispatcher) {
        megaLocalRoomGateway.getCompletedTransfersCount() == 0
    }

    override suspend fun getNumPendingPausedUploads(): Int = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.state == MegaTransfer.STATE_PAUSED
        }
    }

    override suspend fun getNumPendingPausedGeneralUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.state == MegaTransfer.STATE_PAUSED
                    && !transfer.isCUUpload() && !transfer.isChatUpload()
        }
    }

    override suspend fun getNumPendingPausedCameraUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished
                    && transfer.state == MegaTransfer.STATE_PAUSED
                    && transfer.isCUUpload()
        }
    }

    override suspend fun getNumPendingPausedChatUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished
                    && transfer.state == MegaTransfer.STATE_PAUSED
                    && transfer.isChatUpload()
        }
    }


    override suspend fun getNumPendingNonBackgroundPausedDownloads(): Int =
        withContext(ioDispatcher) {
            getDownloadTransfers().count { transfer ->
                !transfer.isFinished && !transfer.isBackgroundTransfer() && transfer.state == MegaTransfer.STATE_PAUSED
            }
        }

    override fun monitorTransferEvents(): Flow<TransferEvent> =
        merge(
            megaApiGateway.globalTransfer.map { event -> transferEventMapper(event) },
            megaApiGateway.globalRequestEvents.mapNotNull { event ->
                pausedTransferEventMapper(event) {
                    getTransferByTag(event.request.transferTag)
                }
            },
        ).flowOn(ioDispatcher)

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

    override fun monitorStorageOverQuota(): Flow<Boolean> =
        appEventGateway.monitorStorageOverQuota()

    override suspend fun broadcastStorageOverQuota() {
        appEventGateway.broadcastStorageOverQuota()
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

    override fun monitorPausedTransfers() = monitorPausedTransfers.asStateFlow()

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

    override fun getAllCompletedTransfers(size: Int?): Flow<List<CompletedTransfer>> =
        megaLocalRoomGateway.getAllCompletedTransfers(size)
            .flowOn(ioDispatcher)

    override suspend fun addCompletedTransfer(transfer: CompletedTransfer) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.addCompletedTransfer(transfer)
            appEventGateway.broadcastCompletedTransfer(transfer)
        }

    override suspend fun addCompletedTransfersIfNotExist(transfers: List<CompletedTransfer>) =
        withContext(ioDispatcher) {
            // remove id field before comparison
            val existingTransfers =
                megaLocalRoomGateway.getAllCompletedTransfers().firstOrNull()
                    .orEmpty().map { it.copy(id = null) }
            transfers.map { it.copy(id = null) }.forEach { transfer ->
                if (!existingTransfers.any { existingTransfer -> existingTransfer == transfer }) {
                    megaLocalRoomGateway.addCompletedTransfer(transfer)
                }
            }
        }

    override suspend fun deleteOldestCompletedTransfers() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueDeleteOldestCompletedTransfersWorkRequest()
    }

    override fun startDownloadWorker() {
        workerManagerGateway.enqueueDownloadsWorkerRequest()
    }

    override fun monitorTransfersFinished() = appEventGateway.monitorTransfersFinished()

    override suspend fun broadcastTransfersFinished(transfersFinishedState: TransfersFinishedState) =
        appEventGateway.broadcastTransfersFinished(transfersFinishedState)

    override fun monitorStopTransfersWork() = appEventGateway.monitorStopTransfersWork()

    override suspend fun broadcastStopTransfersWork() = appEventGateway.broadcastStopTransfersWork()

    override suspend fun resetTotalUploads() = withContext(ioDispatcher) {
        megaApiGateway.resetTotalUploads()
    }

    override suspend fun getActiveTransferByTag(tag: Int) = withContext(ioDispatcher) {
        megaLocalRoomGateway.getActiveTransferByTag(tag)
    }

    override fun getActiveTransfersByType(transferType: TransferType) =
        megaLocalRoomGateway
            .getActiveTransfersByType(transferType)
            .flowOn(ioDispatcher)
            .cancellable()

    override suspend fun getCurrentActiveTransfersByType(transferType: TransferType) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getCurrentActiveTransfersByType(transferType)
        }

    override suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.insertOrUpdateActiveTransfer(activeTransfer)
        }

    override suspend fun updateTransferredBytes(transfer: Transfer) {
        transferredBytesFlow(transfer.transferType).update {
            it + (transfer.tag to transfer.transferredBytes)
        }
    }

    override suspend fun deleteAllActiveTransfersByType(transferType: TransferType) =
        withContext(ioDispatcher) {
            transferredBytesFlow(transferType).value = mapOf()
            megaLocalRoomGateway.deleteAllActiveTransfersByType(transferType)
        }

    override suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.setActiveTransferAsFinishedByTag(tags)
        }

    override fun getActiveTransferTotalsByType(transferType: TransferType): Flow<ActiveTransferTotals> =
        flow {
            val transferredBytesFlow = transferredBytesFlow(transferType)
            emitAll(
                megaLocalRoomGateway.getActiveTransfersByType(transferType).flowOn(ioDispatcher)
                    .combine(transferredBytesFlow) { activeTransfers, transferredBytes ->
                        activeTransferTotalsMapper(transferType, activeTransfers, transferredBytes)
                    }
            )
        }.cancellable()

    override suspend fun getCurrentActiveTransferTotalsByType(transferType: TransferType): ActiveTransferTotals =
        withContext(ioDispatcher) {
            activeTransferTotalsMapper(
                type = transferType,
                list = megaLocalRoomGateway.getCurrentActiveTransfersByType(transferType),
                transferredBytes = transferredBytesFlow(transferType).value
            )
        }

    override suspend fun getCurrentUploadSpeed() = withContext(ioDispatcher) {
        megaApiGateway.currentUploadSpeed
    }

    override suspend fun pauseTransfers(isPause: Boolean): Boolean = withContext(ioDispatcher) {
        val isPauseResponse = suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("pauseTransfers") { it.flag }
            megaApiGateway.pauseTransfers(isPause, listener)
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }

        monitorPausedTransfers.emit(isPauseResponse)
        localStorageGateway.setTransferQueueStatus(isPauseResponse)
        return@withContext isPauseResponse
    }

    override suspend fun deleteAllCompletedTransfers() = withContext(ioDispatcher) {
        megaLocalRoomGateway.deleteAllCompletedTransfers()
    }

    override suspend fun getFailedOrCanceledTransfers(): List<CompletedTransfer> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getCompletedTransfersByState(
                listOf(
                    MegaTransfer.STATE_FAILED,
                    MegaTransfer.STATE_CANCELLED
                )
            )
        }

    override suspend fun deleteFailedOrCanceledTransfers(): List<CompletedTransfer> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.deleteCompletedTransfersByState(
                listOf(
                    MegaTransfer.STATE_FAILED,
                    MegaTransfer.STATE_CANCELLED
                )
            )
        }

    override suspend fun deleteCompletedTransfer(
        transfer: CompletedTransfer,
        isRemoveCache: Boolean,
    ) = withContext(ioDispatcher) {
        if (isRemoveCache) {
            File(transfer.originalPath).takeIf { it.exists() }?.let { cacheFile ->
                if (cacheFile.delete()) {
                    Timber.d("Deleted success, path is $cacheFile")
                } else {
                    Timber.d("Deleted failed, path is $cacheFile")
                }
            }
        }
        megaLocalRoomGateway.deleteCompletedTransfer(transfer)
    }

    override suspend fun pauseTransferByTag(transferTag: Int, isPause: Boolean) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("pauseTransferByTag") { it.flag }
                megaApiGateway.pauseTransferByTag(transferTag, isPause, listener)
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }

    override suspend fun getAllSdTransfers(): List<SdTransfer> =
        megaLocalRoomGateway.getAllSdTransfers()

    override suspend fun deleteSdTransferByTag(tag: Int) {
        megaLocalRoomGateway.deleteSdTransferByTag(tag)
    }

    override suspend fun insertSdTransfer(transfer: SdTransfer) {
        megaLocalRoomGateway.insertSdTransfer(transfer)
    }

    override suspend fun getCompletedTransferById(id: Int) = withContext(ioDispatcher) {
        megaLocalRoomGateway.getCompletedTransferById(id)
    }

    override suspend fun getTotalDownloadsNonBackground() = withContext(ioDispatcher) {
        getDownloadTransfers().count { transfer -> !transfer.isBackgroundTransfer() }
    }

    override suspend fun getCurrentDownloadSpeed() = withContext(ioDispatcher) {
        megaApiGateway.currentDownloadSpeed
    }

    override suspend fun getTotalDownloadedBytes() = withContext(ioDispatcher) {
        megaApiGateway.totalDownloadedBytes
    }

    override suspend fun getTotalDownloadBytes() = withContext(ioDispatcher) {
        megaApiGateway.totalDownloadBytes
    }


    private val transferredBytesFlowMutex = Mutex()
    private suspend fun transferredBytesFlow(transferType: TransferType): MutableStateFlow<Map<Int, Long>> {
        transferredBytesFlowMutex.withLock {
            return transferredBytesFlows[transferType]
                ?: MutableStateFlow<Map<Int, Long>>(mapOf()).also {
                    transferredBytesFlows[transferType] = it
                }
        }
    }
}

private fun MegaTransfer.isCUUpload() =
    this.appData?.contains(AppDataTypeConstants.CameraUpload.sdkTypeValue) == true

private fun MegaTransfer.isChatUpload() =
    this.appData?.contains(AppDataTypeConstants.ChatUpload.sdkTypeValue) == true
