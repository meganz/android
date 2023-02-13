package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.isBackgroundTransfer
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.TransferEventMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.TransferRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default [TransferRepository] implementation.
 *
 * @param megaApiGateway    [MegaApiGateway]
 * @param ioDispatcher      [IoDispatcher]
 * @param dbH               [DatabaseHandler]
 */
internal class DefaultTransfersRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dbH: DatabaseHandler,
    private val transferEventMapper: TransferEventMapper,
    private val appEventGateway: AppEventGateway,
) : TransfersRepository, TransferRepository {

    override suspend fun cancelTransfer(transfer: MegaTransfer) = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.cancelTransfer(
                transfer = transfer,
                listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (request.type == MegaRequest.TYPE_CANCEL_TRANSFER) {
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(Unit))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    }
                )
            )
        }
    }

    override fun startUpload(
        localPath: String,
        parentNode: MegaNode,
        fileName: String?,
        modificationTime: Long,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        cancelToken: MegaCancelToken?,
    ): Flow<GlobalTransfer> = callbackFlow {
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

    private fun uploadListener(
        channel: SendChannel<GlobalTransfer>,
    ) = OptionalMegaTransferListenerInterface(
        onTransferStart = { transfer ->
            channel.trySend(GlobalTransfer.OnTransferStart(transfer))
        },
        onTransferFinish = { transfer, error ->
            channel.trySend(GlobalTransfer.OnTransferFinish(transfer, error))
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

    override suspend fun cancelAllUploadTransfers() = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.cancelAllUploadTransfers(OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (request.type == MegaRequest.TYPE_CANCEL_TRANSFERS) {
                        if (error.errorCode == MegaError.API_OK) {
                            continuation.resumeWith(Result.success(Unit))
                        } else {
                            continuation.failWithError(error)
                        }
                    }
                }
            ))
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
        dbH.isCompletedTransfersEmpty
    }

    override suspend fun areTransfersPaused(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.areTransfersPaused()
    }

    override suspend fun areAllUploadTransfersPaused(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.areUploadTransfersPaused()
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

    override suspend fun cancelTransferByTag(transferTag: Int) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.failWithError(error)
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
}