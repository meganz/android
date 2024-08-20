package mega.privacy.android.data.repository

import android.os.Build
import androidx.work.WorkInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
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
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.mapper.transfer.InProgressTransferMapper
import mega.privacy.android.data.mapper.transfer.PausedTransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferTotalsMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
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
    private val completedTransferMapper: CompletedTransferMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaNodeMapper: MegaNodeMapper,
    private val sdCardGateway: SDCardGateway,
    private val deviceGateway: DeviceGateway,
    private val inProgressTransferMapper: InProgressTransferMapper,
) : TransferRepository {

    private val monitorPausedTransfers = MutableStateFlow(false)

    private val monitorAskedResumeTransfers = MutableStateFlow(false)

    /**
     * To store in progress transfers in memory instead of in database
     */
    private val inProgressTransfersFlow = MutableStateFlow<Map<Int, InProgressTransfer>>(emptyMap())

    /**
     * to store current transferred bytes in memory instead of in database
     */
    private val transferredBytesFlows =
        HashMap<TransferType, MutableStateFlow<Map<Int, Long>>>()

    init {
        //pause transfers if db indicates it should be paused
        scope.launch {
            if (localStorageGateway.getTransferQueueStatus()) {
                pauseTransfers(true)
            }
        }
    }

    override fun startUpload(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long,
        appData: List<TransferAppData>?,
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
            appData = transferAppDataStringMapper(appData),
            isSourceTemporary = isSourceTemporary,
            shouldStartFirst = shouldStartFirst,
            cancelToken = cancelTokenProvider.getOrCreateCancelToken(),
            listener = listener,
        )

        awaitClose {
            megaApiGateway.removeTransferListener(listener)
        }
    }
        .flowOn(ioDispatcher)
        .cancellable()

    override fun startUploadForChat(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        appData: List<TransferAppData.ChatTransferAppData>,
        isSourceTemporary: Boolean,
    ) = callbackFlow {
        val parentNode = megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
        requireNotNull(parentNode)
        require(appData.isNotEmpty())
        val listener = transferListener(channel)

        megaApiGateway.startUploadForChat(
            localPath = localPath,
            parentNode = parentNode,
            fileName = fileName,
            appData = transferAppDataStringMapper(appData),
            isSourceTemporary = isSourceTemporary,
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
        onFolderTransferUpdate = {
                transfer,
                stage,
                folderCount,
                createdFolderCount,
                fileCount,
                currentFolder,
                currentFileLeafName,
            ->
            channel.trySend(
                transferEventMapper(
                    GlobalTransfer.OnFolderTransferUpdate(
                        transfer,
                        stage,
                        folderCount,
                        createdFolderCount,
                        fileCount,
                        currentFolder,
                        currentFileLeafName
                    )
                )
            )
        },
    )

    override fun startDownload(
        node: TypedNode,
        localPath: String,
        appData: TransferAppData?,
        shouldStartFirst: Boolean,
    ) = callbackFlow {
        val megaNode = runCatching { megaNodeMapper(node) }.getOrNull()
            ?: throw NodeDoesNotExistsException()
        val listener = transferListener(channel)
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
        awaitClose {
            megaApiGateway.removeTransferListener(listener)
        }
    }
        .flowOn(ioDispatcher)
        .cancellable()

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

    override suspend fun getNumPendingCameraUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.isCUUpload()
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

    override suspend fun getNumPendingPausedCameraUploads() = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished
                    && transfer.state == MegaTransfer.STATE_PAUSED
                    && transfer.isCUUpload()
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
        )
            .buffer(capacity = Channel.UNLIMITED)
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
        }
    }

    override fun monitorOfflineFileAvailability(): Flow<Long> =
        appEventGateway.monitorOfflineFileAvailability()


    override suspend fun broadcastOfflineFileAvailability(nodeHandle: Long) {
        appEventGateway.broadcastOfflineFileAvailability(nodeHandle)
    }

    override fun monitorTransferOverQuota(): Flow<Boolean> =
        appEventGateway.monitorTransferOverQuota()

    override suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean) {
        appEventGateway.broadcastTransferOverQuota(isCurrentOverQuota)
    }

    override fun monitorStorageOverQuota(): Flow<Boolean> =
        appEventGateway.monitorStorageOverQuota()

    override suspend fun broadcastStorageOverQuota(isCurrentOverQuota: Boolean) {
        appEventGateway.broadcastStorageOverQuota(isCurrentOverQuota)
    }

    override suspend fun cancelTransfers() = withContext(ioDispatcher) {
        cancelTransfersByType(MegaTransfer.TYPE_UPLOAD)
        cancelTransfersByType(MegaTransfer.TYPE_DOWNLOAD)
    }

    private suspend fun cancelTransfersByType(direction: Int) =
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("cancelTransfersByType") {}
            megaApiGateway.cancelTransfers(direction, listener)
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
        }
    }

    override suspend fun moveTransferToLastByTag(transferTag: Int) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("moveTransferToLastByTag") { }
            megaApiGateway.moveTransferToLastByTag(transferTag, listener)
        }
    }

    override suspend fun moveTransferBeforeByTag(transferTag: Int, prevTransferTag: Int) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("moveTransferBeforeByTag") { }
                megaApiGateway.moveTransferBeforeByTag(transferTag, prevTransferTag, listener)
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

    override fun monitorCompletedTransfer(): Flow<Unit> =
        appEventGateway.monitorCompletedTransfer

    override fun monitorCompletedTransfers(size: Int?): Flow<List<CompletedTransfer>> =
        megaLocalRoomGateway.getCompletedTransfers(size)
            .flowOn(ioDispatcher)

    override suspend fun addCompletedTransfers(
        finishEventsAndPaths: Map<TransferEvent.TransferFinishEvent, String?>,
    ) {
        withContext(ioDispatcher) {
            val completedTransfers = finishEventsAndPaths.map { (event, transferPath) ->
                completedTransferMapper(event.transfer, event.error, transferPath)
            }
            megaLocalRoomGateway.addCompletedTransfers(completedTransfers)
            removeInProgressTransfers(finishEventsAndPaths.keys.map { it.transfer.tag }.toSet())
            appEventGateway.broadcastCompletedTransfer()
        }
    }

    override suspend fun addCompletedTransfersIfNotExist(transfers: List<CompletedTransfer>) =
        withContext(ioDispatcher) {
            // remove id field before comparison
            val existingTransfers =
                megaLocalRoomGateway.getCompletedTransfers().firstOrNull()
                    .orEmpty().map { it.copy(id = null) }
            transfers
                .map { it.copy(id = null) }
                .filter { !existingTransfers.any { existingTransfer -> existingTransfer == it } }
                .let { megaLocalRoomGateway.addCompletedTransfers(it) }
        }

    override suspend fun deleteOldestCompletedTransfers() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueDeleteOldestCompletedTransfersWorkRequest()
    }

    override suspend fun startDownloadWorker() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueDownloadsWorkerRequest()
    }

    override suspend fun startChatUploadsWorker() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueChatUploadsWorkerRequest()
    }

    override fun isDownloadsWorkerEnqueuedFlow() =
        workerManagerGateway.monitorDownloadsStatusInfo().map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.ENQUEUED }
        }

    override suspend fun allowUserToSetDownloadDestination(): Boolean = withContext(ioDispatcher) {
        deviceGateway.getSdkVersionInt() < Build.VERSION_CODES.R
    }

    override fun isChatUploadsWorkerEnqueuedFlow() =
        workerManagerGateway.monitorChatUploadsStatusInfo().map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.ENQUEUED }
        }

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

    override suspend fun getCurrentActiveTransfers(): List<ActiveTransfer> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getCurrentActiveTransfers()
        }

    override suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.insertOrUpdateActiveTransfer(activeTransfer)
        }

    override suspend fun insertOrUpdateActiveTransfers(activeTransfers: List<ActiveTransfer>) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.insertOrUpdateActiveTransfers(activeTransfers)
        }

    override suspend fun updateTransferredBytes(transfer: Transfer) {
        if (transfer.transferredBytes == 0L) return
        transferredBytesFlow(transfer.transferType).update {
            it + (transfer.tag to transfer.transferredBytes)
        }
    }

    override suspend fun deleteAllActiveTransfersByType(transferType: TransferType) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.deleteAllActiveTransfersByType(transferType)
            transferredBytesFlow(transferType).value = mapOf()
        }

    override suspend fun deleteAllActiveTransfers() =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.deleteAllActiveTransfers()
            TransferType.entries.forEach {
                transferredBytesFlow(it).value = mapOf()
            }
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
        }

        monitorPausedTransfers.emit(isPauseResponse)
        localStorageGateway.setTransferQueueStatus(isPauseResponse)
        monitorAskedResumeTransfers.emit(false)
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
            }
        }

    override suspend fun getAllSdTransfers(): List<SdTransfer> =
        megaLocalRoomGateway.getAllSdTransfers()

    override suspend fun getSdTransferByTag(tag: Int): SdTransfer? = withContext(ioDispatcher) {
        megaLocalRoomGateway.getSdTransferByTag(tag)
    }

    override suspend fun deleteSdTransferByTag(tag: Int) {
        megaLocalRoomGateway.deleteSdTransferByTag(tag)
    }

    override suspend fun insertSdTransfer(transfer: SdTransfer) {
        megaLocalRoomGateway.insertSdTransfer(transfer)
    }

    override suspend fun getCompletedTransferById(id: Int) = withContext(ioDispatcher) {
        megaLocalRoomGateway.getCompletedTransferById(id)
    }

    override suspend fun getCurrentDownloadSpeed() = withContext(ioDispatcher) {
        megaApiGateway.currentDownloadSpeed
    }

    override suspend fun getOrCreateSDCardTransfersCacheFolder() =
        withContext(ioDispatcher) {
            sdCardGateway.getOrCreateCacheFolder(
                TRANSFERS_SD_TEMPORARY_FOLDER
            )
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

    companion object {
        internal const val TRANSFERS_SD_TEMPORARY_FOLDER = "transfersSdTempMEGA"
    }

    override fun monitorAskedResumeTransfers() = monitorAskedResumeTransfers.asStateFlow()

    override suspend fun setAskedResumeTransfers() {
        monitorAskedResumeTransfers.emit(true)
    }

    override suspend fun startUploadsWorker() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueUploadsWorkerRequest()
    }

    override fun isUploadsWorkerEnqueuedFlow() =
        workerManagerGateway.monitorUploadsStatusInfo().map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.ENQUEUED }
        }

    override suspend fun updateInProgressTransfer(transfer: Transfer) {
        val inProgressTransfer = inProgressTransferMapper(transfer)
        inProgressTransfersFlow.update { inProgressTransfers ->
            inProgressTransfers.toMutableMap().also {
                it[transfer.tag] = inProgressTransfer
            }
        }
    }

    override suspend fun updateInProgressTransfers(transfers: List<Transfer>) {
        val newInProgressTransfers =
            transfers.map { inProgressTransferMapper(it) }.associateBy { it.tag }
        inProgressTransfersFlow.update { inProgressTransfers ->
            inProgressTransfers.toMutableMap().also {
                it.putAll(newInProgressTransfers)
            }
        }
    }

    override fun monitorInProgressTransfers() = inProgressTransfersFlow

    override suspend fun removeInProgressTransfer(tag: Int) {
        if (!inProgressTransfersFlow.value.containsKey(tag)) return
        inProgressTransfersFlow.update { inProgressTransfers ->
            inProgressTransfers.toMutableMap().also {
                it.remove(tag)
            }
        }
    }

    override suspend fun removeInProgressTransfers(tags: Set<Int>) {
        if (tags.isEmpty()) return
        inProgressTransfersFlow.update { inProgressTransfers ->
            inProgressTransfers.filterKeys { it !in tags }
        }
    }
}

private fun MegaTransfer.isBackgroundTransfer() =
    appData?.contains(AppDataTypeConstants.BackgroundTransfer.sdkTypeValue) == true

private fun MegaTransfer.isCUUpload() =
    appData?.contains(AppDataTypeConstants.CameraUpload.sdkTypeValue) == true

private fun MegaTransfer.isChatUpload() =
    appData?.contains(AppDataTypeConstants.ChatUpload.sdkTypeValue) == true
