package mega.privacy.android.data.repository

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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.MapCache
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.mapper.transfer.CompletedTransferPendingTransferMapper
import mega.privacy.android.data.mapper.transfer.InProgressTransferMapper
import mega.privacy.android.data.mapper.transfer.PausedTransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferTotalsMapper
import mega.privacy.android.data.mapper.transfer.upload.MegaUploadOptionsMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.qualifier.DisplayPathFromUriCache
import mega.privacy.android.data.qualifier.ParentNodeCache
import mega.privacy.android.data.qualifier.TransferPathCache
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.times
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.COLLISION_CHECK_FINGERPRINT
import nz.mega.sdk.MegaTransfer.COLLISION_RESOLUTION_NEW_WITH_N
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
@OptIn(ExperimentalTime::class)
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
    private val completedTransferPendingTransferMapper: CompletedTransferPendingTransferMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaNodeMapper: MegaNodeMapper,
    private val deviceGateway: DeviceGateway,
    private val inProgressTransferMapper: InProgressTransferMapper,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val megaUploadOptionsMapper: MegaUploadOptionsMapper,
    @DisplayPathFromUriCache private val displayPathFromUriCache: MapCache<String, String>,
    @ParentNodeCache private val parentNodeCache: MapCache<Long, MegaNode?>,
    @TransferPathCache private val transferPathCache: MapCache<Pair<Long, TransferType>, String>,
) : TransferRepository {

    private val monitorPausedTransfers = MutableStateFlow(false)

    private val monitorAskedResumeTransfers = MutableStateFlow(false)

    private val monitorTransferOverQuotaErrorTimestamp = MutableStateFlow<Instant?>(null)

    private val monitorTransferInErrorStatus = MutableStateFlow(false)

    /**
     * To store in progress transfers in memory instead of in database
     */
    private val inProgressTransfersFlow =
        MutableStateFlow<Map<Long, InProgressTransfer>>(emptyMap())

    /**
     * to store current transferred bytes in memory instead of in database
     */
    private val activeTransfersFlows: Map<TransferType, MutableStateFlow<Map<Long, ActiveTransfer>>> =
        TransferType.entries.associateWith { MutableStateFlow(mapOf()) }

    override var transferOverQuotaTimestamp = AtomicLong()

    init {
        //pause transfers if db indicates it should be paused
        scope.launch {
            monitorFetchNodesFinishUseCase().collect {
                if (localStorageGateway.getTransferQueueStatus()) {
                    pauseTransfers(true)
                }
            }
        }
    }

    override fun startUpload(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long?,
        appData: List<TransferAppData>?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        pitagTrigger: PitagTrigger,
        pitagTarget: PitagTarget,
    ) = callbackFlow {
        val parentNode = megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
        requireNotNull(parentNode)
        val listener = transferListener(channel)

        runCatching {
            megaUploadOptionsMapper(
                fileName = fileName,
                mtime = modificationTime,
                appData = appData,
                isSourceTemporary = isSourceTemporary,
                startFirst = shouldStartFirst,
                pitagTrigger = pitagTrigger,
                pitagTarget = pitagTarget,
            )
        }.onFailure { Timber.e(it) }.getOrNull()?.let { options ->
            Timber.d("Using startUpload with MegaUploadOptions")
            megaApiGateway.startUpload(
                localPath = localPath,
                parent = parentNode,
                cancelToken = cancelTokenProvider.getOrCreateCancelToken(),
                options = options,
                listener = listener,
            )
        } ?: run {
            Timber.d("Using deprecated startUpload as MegaUploadOptions is null")
            megaApiGateway.startUpload(
                localPath = localPath,
                parent = parentNode,
                fileName = fileName,
                mtime = modificationTime,
                appData = transferAppDataStringMapper(appData),
                isSourceTemporary = isSourceTemporary,
                startFirst = shouldStartFirst,
                cancelToken = cancelTokenProvider.getOrCreateCancelToken(),
                listener = listener,
            )
        }
        awaitClose()
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
        appData: List<TransferAppData>?,
        shouldStartFirst: Boolean,
    ) = callbackFlow {
        val megaNode = runCatching { megaNodeMapper(node) }.getOrNull()
            ?: throw NodeDoesNotExistsException()
        val listener = transferListener(channel)

        megaApiGateway.startDownload(
            node = megaNode,
            localPath = localPath,
            fileName = megaNode.name,
            appData = transferAppDataStringMapper(appData),
            startFirst = shouldStartFirst,
            cancelToken = cancelTokenProvider.getOrCreateCancelToken(),
            collisionCheck = COLLISION_CHECK_FINGERPRINT,
            collisionResolution = COLLISION_RESOLUTION_NEW_WITH_N,
            listener = listener,
        )
        awaitClose()
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
                    if (error.errorCode == API_OK) {
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

    override suspend fun ongoingTransfersExist(): Boolean = getNumPendingTransfers() > 0

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
        megaApiGateway.getTransferByTag(transferTag)?.let { transferMapper(it) }
    }

    override suspend fun getTransferByUniqueId(id: Long) = withContext(ioDispatcher) {
        megaApiGateway.getTransferByUniqueId(id)?.let { transferMapper(it) }
    }

    override fun monitorPausedTransfers() = monitorPausedTransfers.asStateFlow()

    override suspend fun resumeTransfersForNotLoggedInInstance() = withContext(ioDispatcher) {
        megaApiGateway.resumeTransfersForNotLoggedInInstance()
    }

    override suspend fun getInProgressTransfersFromSdk(): List<Transfer> =
        withContext(ioDispatcher) {
            val transfers = mutableListOf<Transfer>()
            megaApiGateway.getTransferData()?.let { data ->
                transfers.addAll(
                    (0 until data.numDownloads)
                        .mapNotNull { getTransferByTag(data.getDownloadTag(it)) })
                transfers.addAll(
                    (0 until data.numUploads)
                        .mapNotNull { getTransferByTag(data.getUploadTag(it)) })
            }
            transfers.sortedBy { it.priority }
        }

    override fun monitorCompletedTransfersByStateWithLimit(
        limit: Int,
        vararg states: TransferState,
    ): Flow<List<CompletedTransfer>> =
        megaLocalRoomGateway.monitorCompletedTransfersByStateWithLimit(limit, *states)
            .flowOn(ioDispatcher)

    override suspend fun getCompletedTransfers() =
        megaLocalRoomGateway.getCompletedTransfers()

    override suspend fun addCompletedTransfers(finishEvents: List<TransferEvent.TransferFinishEvent>) {
        withContext(ioDispatcher) {
            val completedTransfers = finishEvents.map { event ->
                if (event.transfer.state == TransferState.STATE_FAILED) {
                    monitorTransferInErrorStatus.update { true }
                }
                completedTransferMapper(event)
            }
            megaLocalRoomGateway.addCompletedTransfers(completedTransfers)
        }
    }

    override suspend fun addCompletedTransferFromFailedPendingTransfer(
        pendingTransfer: PendingTransfer,
        sizeInBytes: Long,
        error: Throwable,
    ) = withContext(ioDispatcher) {
        val completedTransfer =
            completedTransferPendingTransferMapper(pendingTransfer, sizeInBytes, error)
        megaLocalRoomGateway.addCompletedTransfer(completedTransfer)
        removeInProgressTransfers(setOfNotNull(pendingTransfer.transferUniqueId))
        monitorTransferInErrorStatus.update { true }
    }

    override suspend fun addCompletedTransferFromFailedPendingTransfers(
        pendingTransfers: List<PendingTransfer>,
        error: Throwable,
    ) = withContext(ioDispatcher) {
        val completedTransfers = pendingTransfers.map {
            completedTransferPendingTransferMapper(it, 0L, error)
        }
        megaLocalRoomGateway.addCompletedTransfers(completedTransfers)
        removeInProgressTransfers(pendingTransfers.mapNotNull { it.transferUniqueId }.toSet())
        monitorTransferInErrorStatus.update { true }
    }

    override suspend fun deleteOldestCompletedTransfers() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueDeleteOldestCompletedTransfersWorkRequest()
    }

    override suspend fun deleteCompletedTransfersByPath(path: String) = withContext(ioDispatcher) {
        megaLocalRoomGateway.deleteCompletedTransfersByPath(path)
    }

    override suspend fun startDownloadWorker() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueDownloadsWorkerRequest()
    }

    override suspend fun startChatUploadsWorker() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueChatUploadsWorkerRequest()
    }

    override fun monitorIsDownloadsWorkerEnqueued() =
        workerManagerGateway.monitorDownloadsStatusInfo().map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.ENQUEUED }
        }

    override fun monitorIsDownloadsWorkerFinished() =
        workerManagerGateway.monitorDownloadsStatusInfo().map { workInfos ->
            workInfos.any { it.state.isFinished }
        }

    override fun monitorIsChatUploadsWorkerEnqueued() =
        workerManagerGateway.monitorChatUploadsStatusInfo().map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.ENQUEUED }
        }

    override fun monitorIsChatUploadsWorkerFinished() =
        workerManagerGateway.monitorChatUploadsStatusInfo().map { workInfos ->
            workInfos.any { it.state.isFinished }
        }

    override fun monitorActiveTransfersByType(transferType: TransferType) =
        activeTransfersFlow(transferType).map { it.values.toList() }.flowOn(ioDispatcher)


    override suspend fun getActiveTransfersByType(transferType: TransferType) =
        activeTransfersFlow(transferType).value.values.filter { it.transferType == transferType }

    override suspend fun getActiveTransfers() =
        activeTransfersFlows.values.flatMap { it.value.values }

    override suspend fun putActiveTransfer(activeTransfer: Transfer) {
        putActiveTransfers(listOf(activeTransfer))
    }

    override suspend fun putActiveTransfers(activeTransfers: List<ActiveTransfer>) {
        activeTransfers
            .groupBy { it.transferType }
            .mapValues { (transferType, activeTransfers) ->
                val finishedUniqueIds = activeTransfersFlow(transferType).value.values
                    .filter { it.isFinished }
                    .map { it.uniqueId }
                activeTransfers.filter { it.uniqueId !in finishedUniqueIds }
            }
            .filter { (_, activeTransfers) ->
                activeTransfers.isNotEmpty()
            }
            .forEach { (transferType, activeTransfers) ->
                activeTransfersFlow(transferType).update { map ->
                    map + activeTransfers.associateBy { it.uniqueId }
                }
            }
    }

    override suspend fun deleteAllActiveTransfers() =
        withContext(ioDispatcher) {
            TransferType.entries.forEach {
                activeTransfersFlow(it).value = mapOf()
            }
        }

    override suspend fun updateActiveTransfersBytes(transfers: List<Transfer>) =
        withContext(ioDispatcher) {
            val grouped = transfers
                .filterNot { it.transferredBytes == 0L }
                .groupBy { it.transferType }
            grouped.forEach { (transferType, transfersOfThisType) ->
                activeTransfersFlow(transferType).update { map ->
                    map.updateTransferredBytesKeepingConsistentProgress(transfersOfThisType)
                }
            }
        }

    /**
     * Updates this map with uniqueId keys with new and updated transfers while keeping consistent progress, that is, transfer progress not going backwards
     * @param updatedTransfers new and updated transfers
     * @param getMaxValue lambda to get a value with maximum transfer progress given the current value and the updated transfer
     */
    private fun <T, R> Map<Long, T>.updateTransfersKeepingConsistentProgress(
        updatedTransfers: List<R>,
        getMaxValue: (T?, R) -> T,
    ): Map<Long, T> where R : ActiveTransfer {
        val updated: List<Pair<Long, T>> = updatedTransfers.map {
            it.uniqueId to getMaxValue(this[it.uniqueId], it)
        }
        return this + updated
    }

    private fun Map<Long, ActiveTransfer>.updateTransferredBytesKeepingConsistentProgress(
        updatedTransfers: List<ActiveTransfer>,
    ) =
        this.updateTransfersKeepingConsistentProgress(updatedTransfers) { previousTransfer, newTransfer ->
            if (previousTransfer == null || previousTransfer.transferredBytes < newTransfer.transferredBytes) {
                newTransfer
            } else {
                previousTransfer
            }
        }

    private fun Map<Long, InProgressTransfer>.updateInProgressTransfersKeepingConsistentProgress(
        updatedTransfers: List<Transfer>,
    ) =
        this.updateTransfersKeepingConsistentProgress(
            updatedTransfers
        ) { previousValue, newTransfer ->
            inProgressTransferMapper(
                if (previousValue == null || newTransfer.progress.floatValue > previousValue.progress.floatValue) {
                    newTransfer
                } else {
                    newTransfer.copy(transferredBytes = newTransfer.totalBytes * previousValue.progress)
                }
            )
        }

    override fun monitorActiveTransferTotalsByType(transferType: TransferType): Flow<ActiveTransferTotals> =
        activeTransfersFlow(transferType)
            .scan(null as ActiveTransferTotals?) { previousTotals, activeTransfers ->
                activeTransferTotalsMapper(
                    type = transferType,
                    transfers = activeTransfers.values,
                    previousActionGroups = previousTotals?.actionGroups
                )
            }.filterNotNull() //skip first null value

    override suspend fun getCurrentActiveTransferTotalsByType(transferType: TransferType): ActiveTransferTotals =
        withContext(ioDispatcher) {
            activeTransferTotalsMapper(
                type = transferType,
                transfers = getActiveTransfersByType(transferType),
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

    override suspend fun resetPauseTransfers() = withContext(ioDispatcher) {
        monitorPausedTransfers.emit(false)
        localStorageGateway.setTransferQueueStatus(false)
        monitorAskedResumeTransfers.emit(false)
    }

    override suspend fun deleteAllCompletedTransfers() = withContext(ioDispatcher) {
        megaLocalRoomGateway.deleteAllCompletedTransfers()
    }

    override suspend fun deleteFailedOrCancelledTransfers(): List<CompletedTransfer> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.deleteCompletedTransfersByState(
                listOf(
                    MegaTransfer.STATE_FAILED,
                    MegaTransfer.STATE_CANCELLED
                )
            )
        }

    override suspend fun deleteCompletedTransfers() {
        withContext(ioDispatcher) {
            megaLocalRoomGateway.deleteCompletedTransfersByState(
                listOf(MegaTransfer.STATE_COMPLETED)
            )
        }
    }

    override suspend fun deleteCompletedTransfersById(ids: List<Int>) {
        withContext(ioDispatcher) {
            megaLocalRoomGateway.deleteCompletedTransfersById(ids)
        }
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

    override suspend fun getCurrentDownloadSpeed() = withContext(ioDispatcher) {
        megaApiGateway.currentDownloadSpeed
    }

    private fun activeTransfersFlow(transferType: TransferType): MutableStateFlow<Map<Long, ActiveTransfer>> =
        activeTransfersFlows[transferType] ?: error("Unknown transfer type: $transferType")

    override fun monitorAskedResumeTransfers() = monitorAskedResumeTransfers.asStateFlow()

    override suspend fun setAskedResumeTransfers() {
        monitorAskedResumeTransfers.emit(true)
    }

    override suspend fun startUploadsWorker() = withContext(ioDispatcher) {
        workerManagerGateway.enqueueUploadsWorkerRequest()
    }

    override fun monitorIsUploadsWorkerEnqueued() =
        workerManagerGateway.monitorUploadsStatusInfo().map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.ENQUEUED }
        }

    override fun monitorIsUploadsWorkerFinished() =
        workerManagerGateway.monitorUploadsStatusInfo().map { workInfos ->
            workInfos.any { it.state.isFinished }
        }

    override suspend fun updateInProgressTransfers(transfers: List<Transfer>) {
        transfers
            .takeIf { it.isNotEmpty() }?.let { newInProgressTransfers ->
                inProgressTransfersFlow.update { inProgressTransfers ->
                    inProgressTransfers
                        .updateInProgressTransfersKeepingConsistentProgress(newInProgressTransfers)
                }
            }
    }

    override suspend fun updateInProgressTransfers(
        transfersToUpdate: List<Transfer>,
        finishedUniqueIds: List<Long>,
    ) {
        inProgressTransfersFlow.update { current ->
            current.toMutableMap().also { updated ->
                finishedUniqueIds.forEach { updated.remove(it) }
            }.updateInProgressTransfersKeepingConsistentProgress(transfersToUpdate)
        }
    }

    override fun monitorInProgressTransfers() = inProgressTransfersFlow

    override suspend fun removeInProgressTransfers(uniqueIds: Set<Long>) {
        if (uniqueIds.isEmpty()) return
        inProgressTransfersFlow.update { inProgressTransfers ->
            inProgressTransfers.filterKeys { it !in uniqueIds }
        }
    }

    override fun monitorPendingTransfersByType(transferType: TransferType): Flow<List<PendingTransfer>> =
        megaLocalRoomGateway.monitorPendingTransfersByType(transferType)

    override suspend fun getPendingTransfersByType(transferType: TransferType) =
        megaLocalRoomGateway.getPendingTransfersByType(transferType)

    /**
     * Gets pending transfers by state.
     */
    override suspend fun getPendingTransfersByState(pendingTransferState: PendingTransferState) =
        megaLocalRoomGateway.getPendingTransfersByState(pendingTransferState)

    override fun monitorPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ) = megaLocalRoomGateway.monitorPendingTransfersByTypeAndState(
        transferType,
        pendingTransferState
    )

    override suspend fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ) = megaLocalRoomGateway.getPendingTransfersByTypeAndState(
        transferType,
        pendingTransferState
    )

    override suspend fun insertPendingTransfers(pendingTransfer: List<InsertPendingTransferRequest>) {
        megaLocalRoomGateway.insertPendingTransfers(pendingTransfer)
    }

    override suspend fun getPendingTransfersByUniqueId(uniqueId: Long) =
        megaLocalRoomGateway.getPendingTransfersByUniqueId(uniqueId)

    override suspend fun deletePendingTransferByUniqueId(uniqueId: Long) {
        megaLocalRoomGateway.deletePendingTransferByUniqueId(uniqueId)
    }

    override suspend fun deleteAllPendingTransfers() {
        megaLocalRoomGateway.deleteAllPendingTransfers()
    }

    override suspend fun updatePendingTransfers(
        updatePendingTransferRequests: List<UpdatePendingTransferRequest>,
    ) = megaLocalRoomGateway.updatePendingTransfers(*updatePendingTransferRequests.toTypedArray())

    override suspend fun updatePendingTransfer(
        updatePendingTransferRequest: UpdatePendingTransferRequest,
    ) = megaLocalRoomGateway.updatePendingTransfers(updatePendingTransferRequest)

    override suspend fun getBandwidthOverQuotaDelay() = withContext(ioDispatcher) {
        megaApiGateway.getBandwidthOverQuotaDelay().seconds
    }

    override suspend fun insertActiveTransferGroup(activeTransferActionGroup: ActiveTransferActionGroup) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.insertActiveTransferGroup(activeTransferActionGroup)
        }

    override suspend fun getActiveTransferGroupById(id: Int): ActiveTransferActionGroup? =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getActiveTransferGroup(id)
        }

    override suspend fun getActiveTransferGroups() =
        megaLocalRoomGateway.getActiveTransferGroups()

    override suspend fun deleteActiveTransferGroup(id: Int) {
        megaLocalRoomGateway.deleteActiveTransferGroup(id)
    }


    override suspend fun broadcastTransferTagToCancel(transferTag: Int?) {
        withContext(ioDispatcher) {
            appEventGateway.broadcastTransferTagToCancel(transferTag)
        }
    }

    override fun monitorTransferTagToCancel(): Flow<Int?> =
        appEventGateway.monitorTransferTagToCancel()

    override fun monitorTransferOverQuotaErrorTimestamp() =
        monitorTransferOverQuotaErrorTimestamp.asStateFlow()

    override suspend fun setTransferOverQuotaErrorTimestamp() {
        monitorTransferOverQuotaErrorTimestamp.emit(
            Instant.fromEpochMilliseconds(deviceGateway.getCurrentTimeInMillis())
        )
    }

    override fun monitorTransferInErrorStatus() = monitorTransferInErrorStatus.asStateFlow()

    override fun clearTransferErrorStatus() {
        monitorTransferInErrorStatus.update { false }
    }

    private fun MegaTransfer.isBackgroundTransfer() =
        appData?.contains(AppDataTypeConstants.BackgroundTransfer.sdkTypeValue) == true

    private fun MegaTransfer.isCUUpload() =
        appData?.contains(AppDataTypeConstants.CameraUpload.sdkTypeValue) == true

    override fun clearCompletedTransfersCache() {
        displayPathFromUriCache.clear()
        parentNodeCache.clear()
        transferPathCache.clear()
    }
}
