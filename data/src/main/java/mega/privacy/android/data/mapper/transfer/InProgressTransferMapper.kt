package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import javax.inject.Inject

/**
 * [Transfer] to [InProgressTransfer] mapper
 */
internal class InProgressTransferMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param transfer [Transfer]
     * @return [InProgressTransfer]
     */
    operator fun invoke(transfer: Transfer) = with(transfer) {
        if (transferType.isDownloadType()) {
            InProgressTransfer.Download(
                tag = tag,
                totalBytes = totalBytes,
                fileName = fileName,
                speed = speed,
                state = state,
                priority = priority,
                isPaused = isPaused,
                progress = progress,
                nodeId = NodeId(nodeHandle),
            )
        } else {
            InProgressTransfer.Upload(
                tag = tag,
                totalBytes = totalBytes,
                fileName = fileName,
                speed = speed,
                state = state,
                priority = priority,
                isPaused = isPaused,
                progress = progress,
                localPath = localPath,
            )
        }
    }
}
