package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.extensions.isBackgroundTransfer
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.TransfersRepository
import mega.privacy.android.domain.entity.TransfersSizeInfo
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import javax.inject.Inject

/**
 * Default [TransfersRepository] implementation.
 *
 * @param megaApiGateway    [MegaApiGateway]
 * @param ioDispatcher      [IoDispatcher]
 * @param dbH               [DatabaseHandler]
 */
class DefaultTransfersRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dbH: DatabaseHandler,
) : TransfersRepository {
    private val transferMap: MutableMap<Int, MegaTransfer> = hashMapOf()

    override suspend fun getUploadTransfers(): List<MegaTransfer> = withContext(ioDispatcher) {
        megaApiGateway.getTransfers(MegaTransfer.TYPE_UPLOAD)
    }

    override suspend fun getDownloadTransfers(): List<MegaTransfer> = withContext(ioDispatcher) {
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
        dbH.completedTransfers.isEmpty()
    }

    override suspend fun areTransfersPaused(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.areTransfersPaused()
    }

    override suspend fun getNumPendingPausedUploads(): Int = withContext(ioDispatcher) {
        getUploadTransfers().count { transfer ->
            !transfer.isFinished && transfer.state == MegaTransfer.STATE_PAUSED
        }
    }

    override suspend fun getNumPendingNonBackgroundPausedUploads(): Int =
        withContext(ioDispatcher) {
            getDownloadTransfers().count { transfer ->
                !transfer.isFinished && !transfer.isBackgroundTransfer() && transfer.state == MegaTransfer.STATE_PAUSED
            }
        }

    override suspend fun areAllTransfersPaused(): Boolean = withContext(ioDispatcher) {
        areTransfersPaused() || getNumPendingPausedUploads() + getNumPendingNonBackgroundPausedUploads() == getNumPendingTransfers()
    }

    override fun getSizeTransfer(): Flow<TransfersSizeInfo> = megaApiGateway.globalTransfer
        .map {
            val transfer = it.transfer
            if (transfer != null) {
                transferMap[transfer.tag] = transfer
            }

            var totalBytes: Long = 0
            var totalTransferred: Long = 0

            val megaTransfers = transferMap.values.toList()
            for (currentTransfer in megaTransfers) {
                if (currentTransfer.state == MegaTransfer.STATE_COMPLETED) {
                    totalBytes += currentTransfer.totalBytes
                    totalTransferred += currentTransfer.totalBytes
                } else {
                    totalBytes += currentTransfer.totalBytes
                    totalTransferred += currentTransfer.transferredBytes
                }
            }
            // we only clear cache when all transfer done
            // if we remove in OnTransferFinish it can cause the progress show incorrectly
            if (megaTransfers.all { megaTransfer -> megaTransfer.isFinished }) {
                transferMap.clear()
            }
            Timber.d("Total transfer ${transferMap.size}")
            TransfersSizeInfo(
                transferType = transfer?.type ?: -1,
                totalSizePendingTransfer = totalBytes,
                totalSizeTransferred = totalTransferred
            )
        }
}