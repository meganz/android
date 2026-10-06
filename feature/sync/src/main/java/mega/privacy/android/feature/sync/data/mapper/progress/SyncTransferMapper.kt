package mega.privacy.android.feature.sync.data.mapper.progress

import mega.privacy.android.data.mapper.transfer.TransferStateMapper
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * Maps a sync [MegaTransfer] to a [SyncTransferEntry], reading each getter at most once.
 * The path is read only when [invoke]'s `readPath` is set or the transfer has ended, so active rows stay small.
 */
internal class SyncTransferMapper @Inject constructor(
    private val transferStateMapper: TransferStateMapper,
) {
    operator fun invoke(transfer: MegaTransfer, readPath: Boolean): SyncTransferEntry {
        val state = transferStateMapper(transfer.state)
        val direction = if (transfer.type == MegaTransfer.TYPE_DOWNLOAD) {
            SyncTransferDirection.Download
        } else {
            SyncTransferDirection.Upload
        }
        return SyncTransferEntry(
            tag = transfer.tag,
            direction = direction,
            state = state,
            fileName = transfer.fileName.orEmpty(),
            totalBytes = transfer.totalBytes,
            transferredBytes = transfer.transferredBytes,
            speed = transfer.speed,
            meanSpeed = transfer.meanSpeed,
            priority = transfer.priority.toLong(),
            notificationNumber = transfer.notificationNumber,
            lastError = transfer.lastErrorExtended?.let { error ->
                error.errorCode.takeIf { it != MegaError.API_OK }
                    ?.let { SyncTransferError(code = it, value = error.value) }
            },
            isForeignOverQuota = direction == SyncTransferDirection.Upload && transfer.isForeignOverquota,
            path = if (readPath || state in TERMINAL_STATES) transfer.path else null,
        )
    }

    private companion object {
        val TERMINAL_STATES = setOf(
            TransferState.STATE_COMPLETED,
            TransferState.STATE_CANCELLED,
            TransferState.STATE_FAILED,
        )
    }
}
