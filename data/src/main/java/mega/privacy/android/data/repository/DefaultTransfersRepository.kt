package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.isBackgroundTransfer
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.TransferEventMapper
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.TransferRepository
import nz.mega.sdk.MegaError
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
}