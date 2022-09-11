package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.repository.TransferRepository

/**
 * Default implementation of [MonitorTransfersSize]
 */
class DefaultMonitorTransfersSize(
    private val repository: TransferRepository,
) : MonitorTransfersSize {
    private val transferMap: MutableMap<Int, Transfer> = hashMapOf()

    override fun invoke(): Flow<TransfersSizeInfo> = repository.monitorTransferEvents()
        .map {
            val transfer = it.transfer
            transferMap[transfer.tag] = transfer

            var totalBytes: Long = 0
            var totalTransferred: Long = 0

            val megaTransfers = transferMap.values.toList()
            megaTransfers.forEach { (bytes, transferredBytes, transferState) ->
                totalBytes += bytes
                totalTransferred +=
                    if (transferState == TransferState.STATE_COMPLETED) bytes
                    else transferredBytes
            }
            // we only clear cache when all transfer done
            // if we remove in OnTransferFinish it can cause the progress show incorrectly
            if (megaTransfers.all { megaTransfer -> megaTransfer.isFinished }) {
                transferMap.clear()
            }
            TransfersSizeInfo(
                transferType = transfer.transferType,
                totalSizePendingTransfer = totalBytes,
                totalSizeTransferred = totalTransferred
            )
        }
}